package main.processes

import akka.actor.{Actor, ActorRef}
import akka.event.slf4j.Logger
import main.utility.{EchoWave, EchoTerminate, MessageTypes, AllProcessesCreated}

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
  var received: Int = 0 // Counter for received EchoWave messages
  var parent: Option[String] = None // Optional parent process ID

  /**
   * Receives messages and performs actions based on the message type.
   */
  def receive: Receive = {
    case AllProcessesCreated =>
      if (initiator) {
        // If this process is the initiator and all processes are created
        // Send EchoWave messages to all neighbors
        log.info(s"$self is the initiator, all processes created, sending Wave to neighbors: $neighbors")
        neighbors.foreach(neighbor => context.actorSelection(s"/user/$neighbor") ! EchoWave())
      }

    case EchoWave() =>
      // Received an EchoWave message
      val senderId = getSender(context.sender())
      received += 1
      log.info(s"$self received Wave from $senderId, received count: $received")

      if (parent.isEmpty && !initiator) {
        // If no parent is set and this process is not the initiator
        // Set the sender as the parent
        parent = Some(senderId)
        log.info(s"$self set $senderId as parent")

        if (neighbors.size > 1) {
          // If there are more than one neighbor
          // Send EchoWave messages to all neighbors except the parent
          neighbors.foreach { neighbor =>
            if (neighbor != senderId) {
              log.info(s"$self sending Wave to $neighbor")
              context.actorSelection(s"/user/$neighbor") ! EchoWave()
            }
          }
        } else {
          // If there is only one neighbor (the parent)
          // Send EchoWave message back to the parent
          log.info(s"$self sending Wave back to $senderId")
          context.actorSelection(s"/user/$senderId") ! EchoWave()
        }
      } else if (received == neighbors.size) {
        // If received EchoWave messages from all neighbors
        parent match {
          case Some(parentId) =>
            // If this process is not the initiator
            // Send EchoWave message back to the parent
            log.info(s"$self received Wave from all neighbors, sending Wave to parent $parentId")
            context.actorSelection(s"/user/$parentId") ! EchoWave()
          case None =>
            // If this process is the initiator and received EchoWave messages from all neighbors
            // Send EchoTerminate message to the terminator and stop the process
            log.info(s"$self (initiator) received Wave from all neighbors, deciding")
            context.actorSelection("/user/terminator") ! EchoTerminate()
            context.stop(self)
        }
      }
  }

  /**
   * Utility method to get the sender ID from the ActorRef.
   *
   * @param actorRef The ActorRef of the sender.
   * @return The sender ID extracted from the ActorRef's path name.
   */
  def getSender(actorRef: ActorRef): String = {
    actorRef.path.name
  }
}