package main.processes

import akka.actor.{Actor, ActorSystem}
import akka.event.slf4j.Logger
import main.utility._

/**
 * Actor representing a process in the Echo algorithm.
 *
 * @param id            The unique identifier of the process.
 * @param neighbor      The list of neighboring process IDs.
 * @param processRecord The ProcessRecord instance for storing process references.
 */
class ChangRobertsProcess(val id: Int, val uid: Int, val neighbor: Int, val system: ActorSystem, val processRecord: ProcessRecord) extends Actor {
  // Logger for logging messages
  val logger = Logger(getClass.getName)

  // Flag to indicate if the process is participating in the election
  var participant = false

  // Variable to store the elected leader's UID (if any)
  var electedLeader: Option[Int] = None

  // Overriding the preStart method to perform any initialization before the actor starts
  override def preStart(): Unit = {
    super.preStart()
  }

  /**
   * Receives messages and performs actions based on the message type.
   */
  def receive: Receive = {
    // Handling the Initiate message
    case Initiate =>
      // Logging that the process has been initiated
      logger.info(s"Initiated by process $self")

      // Setting the participant flag to true
      participant = true

      // Sending an ElectionMessage to the neighbor process with the current process's UID
      processRecord.map(neighbor) ! ElectionMessage(uid)

    // Handling the ElectionMessage
    case ElectionMessage(senderUid) =>
      // Getting the sender actor reference
      val senderActor = sender()

      // Logging the received ElectionMessage with the sender's UID
      logger.info(s"$self received ElectionMessage from $senderActor with UID $senderUid")

      // Comparing the received UID with the current process's UID
      if (senderUid > uid) {
        // If the received UID is greater, forwarding the ElectionMessage to the neighbor process
        logger.info(s"$self forwarding ElectionMessage with UID $senderUid to neighbor ${processRecord.map(neighbor)}")
        processRecord.map(neighbor) ! ElectionMessage(senderUid)
      } else if (senderUid < uid && !participant) {
        // If the received UID is smaller and the process is not a participant,
        // setting the participant flag to true and replacing the UID in the message with the current process's UID
        participant = true
        logger.info(s"$self replacing UID in the message and forwarding ElectionMessage with UID $uid to neighbor ${processRecord.map(neighbor)}")
        processRecord.map(neighbor) ! ElectionMessage(uid)
      } else if (senderUid < uid && participant) {
        // If the received UID is smaller and the process is already a participant, discarding the message
        logger.info(s"$self discarding the ElectionMessage")
      } else if (senderUid == uid) {
        // If the received UID is equal to the current process's UID, the process is elected as the leader
        logger.info(s"$self elected as leader")
        electedLeader = Some(uid)
        announceLeader()
      }

    // Handling the ElectedMessage
    case ElectedMessage(leaderUid) =>
      if (leaderUid != uid) {
        // If the received leader UID is different from the current process's UID,
        // setting the participant flag to false and updating the electedLeader variable
        participant = false
        electedLeader = Some(leaderUid)

        // Forwarding the ElectedMessage to the neighbor process
        logger.info(s"$self forwarding ElectedMessage with leader UID $leaderUid to neighbor ${processRecord.map(neighbor)}")
        processRecord.map(neighbor) ! ElectedMessage(leaderUid)
      } else {
        // If the received leader UID is equal to the current process's UID, discarding the message
        logger.info(s"$self discarding the ElectedMessage")

        // Logging that the election is over and the current process is the leader
        logger.info(s"Election is over. Leader: $self, UID: $leaderUid")

        // Sending a TerminateSystem message to the process with ID -1 (assuming it's a special process for system termination)
        processRecord.map(-1) ! TerminateSystem
      }
  }

  // Private method to announce the current process as the leader
  private def announceLeader(): Unit = {
    // Setting the participant flag to false
    participant = false

    // Logging that the current process is announcing itself as the leader and sending an ElectedMessage to the neighbor process
    logger.info(s"$self announcing itself as the leader and sending ElectedMessage to neighbor ${processRecord.map(neighbor)}")
    processRecord.map(neighbor) ! ElectedMessage(uid)
  }
}