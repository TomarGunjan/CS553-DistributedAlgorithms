package main.processes

import akka.actor.Actor
import akka.event.slf4j.Logger
import main.utility.{InitiateTarry, TarryProbe, TerminateTarry, MessageTypes, ProcessRecord}

import scala.collection.mutable
import scala.util.Random

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
  var sent: mutable.Set[Int] = mutable.Set.empty // Set of neighbor IDs to which tokens have been sent

  /**
   * Receives messages and performs actions based on the message type.
   */
  def receive: Receive = {
    case InitiateTarry =>
      log.info(s"$self is the initiator")
      if (neighbors.nonEmpty) {
        val firstNeighbor = neighbors.head
        forward(firstNeighbor) // Forward the token to the first neighbor
      }

    case TarryProbe(sid) =>
      log.info(s"$self received token from ${sender()}")
      tokensReceived += 1

      if (parent == -1) {
        parent = sid // Set the sender as the parent if no parent is assigned yet
      }

      if (initiator && tokensSent == neighbors.size && tokensReceived == neighbors.size) {
        // If this process is the initiator and has sent and received tokens through all channels
        log.info(s"$self received token through all channels, traversal completed")
        processRecord.map(-1) ! TerminateTarry // Send termination message to the terminator
      } else {
        val unsentNeighbors = neighbors.filter(n => !sent.contains(n) && n != sid) // Find unsent neighbors excluding the sender
        if (unsentNeighbors.nonEmpty) {
          val nextNeighbor = Random.shuffle(unsentNeighbors).head // Randomly select the next unsent neighbor
          forward(nextNeighbor) // Forward the token to the randomly selected unsent neighbor
        } else if (tokensSent == neighbors.size) {
          // Forward the token back to the parent only if all neighbors have been sent tokens
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
        actorRef ! TarryProbe(id) // Send a TarryProbe message to the neighbor
        tokensSent += 1
        sent += neighborId // Add the neighbor ID to the sent set
      case None =>
        log.error(s"$self Actor reference not found for process $neighborId")
    }
  }
}
