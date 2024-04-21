package main.processes

import akka.actor.{Actor, ActorRef}
import akka.event.slf4j.Logger
import main.utility.{Decide, Info, Wave, TreeTerminate, MessageTypes, ProcessRecord}

class TreeProcess(val id: Int, val neighbors: List[Int], val processRecord: ProcessRecord)
  extends Actor with MessageTypes {
  val log = Logger(getClass.getName)

  var received: Map[Int, Boolean] = neighbors.map(_ -> false).toMap
  var parent: Option[Int] = None

  log.info(s"[$self] initialized with neighbors: $neighbors")

  def receive: Receive = {
    case Wave =>
      val senderId = getSender(context.sender())
      received += (senderId -> true)
      log.info(s"[$self] received Wave from [${context.sender()}]")
      sendWave()

    case Info =>
      val senderId = getSender(context.sender())
      log.info(s"[$self] received Info from [${context.sender()}]")
      if (parent.contains(senderId)) {
        neighbors.foreach { neighbor =>
          if (neighbor != senderId) {
            log.info(s"[$self] sending Info to [${processRecord.map(neighbor)}]")
            processRecord.map(neighbor) ! Info
          }
        }
      }

    case Decide =>
      log.info(s"[$self] decided")
      processRecord.map(-1) ! TreeTerminate
  }

  def sendWave(): Unit = {
    val unreceivedNeighbors = neighbors.filterNot(received)
    if (unreceivedNeighbors.size == 1) {
      val neighbor = unreceivedNeighbors.head
      log.info(s"[$self] sending Wave to [${processRecord.map(neighbor)}]")
      processRecord.map(neighbor) ! Wave
      parent = Some(neighbor)
      log.info(s"[$self] set [${processRecord.map(neighbor)}] as parent")
    } else if (unreceivedNeighbors.isEmpty) {
      log.info(s"[$self] sending Decide to itself")
      self ! Decide
      neighbors.foreach { neighbor =>
        if (neighbor != parent.getOrElse(-1)) {
          log.info(s"[$self] sending Info to [${processRecord.map(neighbor)}]")
          processRecord.map(neighbor) ! Info
        }
      }
    }
  }

  def getSender(actorRef: ActorRef): Int = {
    processRecord.map.find(_._2 == actorRef).map(_._1).getOrElse(-1)
  }
}