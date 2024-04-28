package main.processes

import akka.actor.Actor
import akka.event.slf4j.Logger
import main.utility.{InitiateTarry, ProcessRecord, TarryProbe, TerminateTarry}

import scala.collection.mutable

/**
 * Actor representing a process in Tarry's algorithm.
 *
 * @param id            The unique identifier of the process.
 * @param neighbors     The list of neighboring process IDs.
 * @param initiator     Flag indicating if this process is the initiator.
 * @param processRecord The ProcessRecord instance for storing process references.
 */
class TarryProcess(val id: Int, val neighbors: List[Int], val initiator: Boolean, val processRecord: ProcessRecord)
  extends Actor {
  val log = Logger(getClass.getName)
  var parent: Int = -1 // The parent process ID
  var tokensSent: Int = 0 // Counter for tokens sent
  var tokensReceived: Int = 0 // Counter for tokens received
  var sent: mutable.Set[Int] = mutable.Set.empty // Set of neighbor IDs to which the token has been sent

  /**
   * Receives messages and performs actions based on the message type.
   */
  def receive: Receive = {
    case InitiateTarry =>
      log.info(s"$self is the initiator")
      if (neighbors.nonEmpty) {
        // If there are neighbors, forward the token to the first neighbor
        val firstNeighbor = neighbors.head
        forward(firstNeighbor)
      }

    case TarryProbe(sid) =>
      log.info(s"$self received token from ${sender()}")
      tokensReceived += 1

      if (parent == -1) {
        // Set the sender as the parent if not already set
        parent = sid
      }

      if (neighbors.forall(sent.contains)) {
        // All neighbors have been visited
        if (initiator) {
          // If this process is the initiator, terminate the algorithm
          log.info(s"$self received token through all channels, traversal completed")
          processRecord.map(-1) ! TerminateTarry
        } else {
          // If this process is not the initiator, send the token back to the parent
          forward(parent)
        }
      } else {
        // Choose the next unsent neighbor according to the rules
        val unsentNeighbors = neighbors.filter(n => !sent.contains(n) && n != parent)
        if (unsentNeighbors.nonEmpty) {
          // If there are unsent neighbors, forward the token to the first unsent neighbor
          val nextNeighbor = unsentNeighbors.head
          forward(nextNeighbor)
        } else {
          // If there are no unsent neighbors, send the token back to the parent
          forward(parent)
        }
      }
  }

  /**
   * Forwards the token to the specified neighbor.
   *
   * @param neighborId The ID of the neighbor to forward the token to.
   */
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