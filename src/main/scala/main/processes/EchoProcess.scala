package main.processes

import akka.actor.{Actor, ActorRef}
import akka.event.slf4j.Logger
import main.utility.{EchoWave, EchoTerminate, MessageTypes}

/**
 * Actor representing a process in the Echo algorithm.
 *
 * @param id        The unique identifier of the process.
 * @param neighbors The list of neighboring process IDs.
 * @param initiator Flag indicating if this process is the initiator.
 */
class EchoProcess(val id: String, val neighbors: List[String], val initiator: Boolean)
  extends Actor {
  val log = Logger(getClass.getName)
  var received: Int = 0 // Counter for received Wave messages
  var parent: Option[String] = None // Optional parent process ID

  // If this process is the initiator, send Wave messages to all neighbors
  if (initiator) {
    log.info(s"$self is the initiator, sending Wave to neighbors: $neighbors")
    neighbors.foreach(neighbor => context.actorSelection(s"/user/$neighbor") ! EchoWave())
  }

  /**
   * Receives messages and performs actions based on the message type.
   */
  def receive: Receive = {
    case EchoWave() =>
      val senderId = getSender(context.sender())
      received += 1
      log.info(s"$self received Wave from $senderId, received count: $received")

      // If no parent is set and this process is not the initiator
      if (parent.isEmpty && !initiator) {
        parent = Some(senderId) // Set the sender as the parent
        log.info(s"$self set $senderId as parent")

        // If there are more than one neighbor
        if (neighbors.size > 1) {
          // Send Wave messages to all neighbors except the parent
          neighbors.foreach { neighbor =>
            if (neighbor != senderId) {
              log.info(s"$self sending Wave to $neighbor")
              context.actorSelection(s"/user/$neighbor") ! EchoWave()
            }
          }
        } else {
          // If there is only one neighbor, send Wave back to the parent
          log.info(s"$self sending Wave back to $senderId")
          context.actorSelection(s"/user/$senderId") ! EchoWave()
        }
      } else if (received == neighbors.size) {
        // If Wave messages have been received from all neighbors
        parent match {
          case Some(parentId) =>
            // If this process has a parent, send Wave to the parent
            log.info(s"$self received Wave from all neighbors, sending Wave to parent $parentId")
            context.actorSelection(s"/user/$parentId") ! EchoWave()
          case None =>
            // If this process is the initiator and has received Wave from all neighbors
            log.info(s"$self (initiator) received Wave from all neighbors, deciding")
            context.actorSelection("/user/terminator") ! EchoTerminate() // Send termination message to the terminator
            context.stop(self) // Stop this actor
        }
      }
  }

  /**
   * Utility method to extract the sender ID from an ActorRef.
   *
   * @param actorRef The ActorRef of the sender.
   * @return The sender ID.
   */
  def getSender(actorRef: ActorRef): String = {
    actorRef.path.name
  }
}