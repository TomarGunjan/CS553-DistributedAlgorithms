package main.processes

import akka.actor.Actor
import akka.event.slf4j.Logger
import main.utility.{InitiateTarry,TarryProbe, TerminateTarry, MessageTypes, ProcessRecord}

import scala.collection.mutable

class TarryProcess(val id: Int, val neighbors: List[Int], val initiator: Boolean, val processRecord: ProcessRecord)
  extends Actor with MessageTypes {  val log = Logger(getClass.getName)
  var parent: Int = -1
  var tokensSent: Int = 0
  var tokensReceived: Int = 0
  var sent: mutable.Set[Int] = mutable.Set.empty

  def receive: Receive = {
    case InitiateTarry =>
      log.info(s"$self is the initiator")
      if (neighbors.nonEmpty) {
        val firstNeighbor = neighbors.head
        forward(firstNeighbor)
      }

    case TarryProbe(sid) =>
      log.info(s"$self received token from ${sender()}")
      tokensReceived += 1

      if (parent == -1) {
        parent = sid
      }

      if (initiator && tokensSent == neighbors.size && tokensReceived == neighbors.size) {
        log.info(s"$self received token through all channels, traversal completed")
        processRecord.map(-1) ! TerminateTarry
      } else {
        val unsentNeighbors = neighbors.filter(n => !sent.contains(n))
        if (unsentNeighbors.nonEmpty) {
          val nextNeighbor = unsentNeighbors.head
          forward(nextNeighbor)
        } else if (sid != parent) {
          forward(parent)
        }
      }
  }

  def forward(neighborId: Int): Unit = {
    processRecord.map.get(neighborId) match {
      case Some(actorRef) =>
        log.info(s"$self forwarding token to $actorRef")
        actorRef ! TarryProbe(id)
        tokensSent += 1
        sent += neighborId
      case None =>
        log.error(s"$self Actor reference not found for process $neighborId")
    }
  }
}