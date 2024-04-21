package main.processes

import akka.actor.{Actor, ActorRef}
import akka.event.slf4j.Logger
import main.utility.{Decide, Info, Wave, TreeTerminate, MessageTypes, ProcessRecord}

/**
 * Actor representing a process in the tree algorithm.
 *
 * @param id            The unique identifier of the process.
 * @param neighbors     The list of neighboring process IDs.
 * @param processRecord The ProcessRecord instance for storing process references.
 */
class TreeProcess(val id: Int, val neighbors: List[Int], val processRecord: ProcessRecord)
  extends Actor {
  val log = Logger(getClass.getName)

  var received: Map[Int, Boolean] = neighbors.map(_ -> false).toMap // Map to keep track of received messages from neighbors
  var parent: Option[Int] = None // Optional parent process ID

  log.info(s"[$self] initialized with neighbors: $neighbors")

  /**
   * Receives messages and performs actions based on the message type.
   */
  def receive: Receive = {
    case Wave =>
      val senderId = getSender(context.sender())
      received += (senderId -> true) // Mark the sender as having sent a Wave message
      log.info(s"[$self] received Wave from [${context.sender()}]")
      sendWave() // Send Wave messages to unreceived neighbors

    case Info =>
      val senderId = getSender(context.sender())
      log.info(s"[$self] received Info from [${context.sender()}]")
      if (parent.contains(senderId)) {
        // If the sender is the parent, forward Info message to other neighbors
        neighbors.foreach { neighbor =>
          if (neighbor != senderId) {
            log.info(s"[$self] sending Info to [${processRecord.map(neighbor)}]")
            processRecord.map(neighbor) ! Info
          }
        }
      }

    case Decide =>
      log.info(s"[$self] decided")
      processRecord.map(-1) ! TreeTerminate // Send termination message to the terminator
  }

  /**
   * Sends Wave messages to unreceived neighbors.
   * If there is only one unreceived neighbor, it becomes the parent and a Wave message is sent to it.
   * If there are no unreceived neighbors, the process decides and sends Info messages to all neighbors except the parent.
   */
  def sendWave(): Unit = {
    val unreceivedNeighbors = neighbors.filterNot(received)
    if (unreceivedNeighbors.size == 1) {
      val neighbor = unreceivedNeighbors.head
      log.info(s"[$self] sending Wave to [${processRecord.map(neighbor)}]")
      processRecord.map(neighbor) ! Wave
      parent = Some(neighbor) // Set the unreceived neighbor as the parent
      log.info(s"[$self] set [${processRecord.map(neighbor)}] as parent")
    } else if (unreceivedNeighbors.isEmpty) {
      log.info(s"[$self] sending Decide to itself")
      self ! Decide // Send Decide message to itself
      neighbors.foreach { neighbor =>
        if (neighbor != parent.getOrElse(-1)) {
          log.info(s"[$self] sending Info to [${processRecord.map(neighbor)}]")
          processRecord.map(neighbor) ! Info // Send Info message to all neighbors except the parent
        }
      }
    }
  }

  /**
   * Utility method to get the sender ID from the ActorRef.
   *
   * @param actorRef The ActorRef of the sender.
   * @return The sender ID if found, or -1 if not found.
   */
  def getSender(actorRef: ActorRef): Int = {
    processRecord.map.find(_._2 == actorRef).map(_._1).getOrElse(-1)
  }
}