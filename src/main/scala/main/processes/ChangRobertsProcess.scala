package main.processes

import akka.actor.{Actor, ActorSystem}
import akka.event.slf4j.Logger
import main.utility._

class ChangRobertsProcess(val id: Int, val uid: Int, val neighbor: Int, val system: ActorSystem, val processRecord: ProcessRecord) extends Actor {
  val logger = Logger(getClass.getName)
  var participant = false
  var electedLeader: Option[Int] = None

  override def preStart(): Unit = {
    super.preStart()
  }

  def receive: Receive = {
    case Initiate =>
      logger.info(s"Initiated by process $self")
      participant = true
      processRecord.map(neighbor) ! ElectionMessage(uid)

    case ElectionMessage(senderUid) =>
      val senderActor = sender()
      logger.info(s"$self received ElectionMessage from $senderActor with UID $senderUid")
      if (senderUid > uid) {
        logger.info(s"$self forwarding ElectionMessage with UID $senderUid to neighbor ${processRecord.map(neighbor)}")
        processRecord.map(neighbor) ! ElectionMessage(senderUid)
      } else if (senderUid < uid && !participant) {
        participant = true
        logger.info(s"$self replacing UID in the message and forwarding ElectionMessage with UID $uid to neighbor ${processRecord.map(neighbor)}")
        processRecord.map(neighbor) ! ElectionMessage(uid)
      } else if (senderUid < uid && participant) {
        logger.info(s"$self discarding the ElectionMessage")
      } else if (senderUid == uid) {
        logger.info(s"$self elected as leader")
        electedLeader = Some(uid)
        announceLeader()
      }

    case ElectedMessage(leaderUid) =>
      if (leaderUid != uid) {
        participant = false
        electedLeader = Some(leaderUid)
        logger.info(s"$self forwarding ElectedMessage with leader UID $leaderUid to neighbor ${processRecord.map(neighbor)}")
        processRecord.map(neighbor) ! ElectedMessage(leaderUid)
      } else {
        logger.info(s"$self discarding the ElectedMessage")
        logger.info(s"Election is over. Leader: $self, UID: $leaderUid")
        processRecord.map(-1) ! TerminateSystem
      }
  }

  private def announceLeader(): Unit = {
    participant = false
    logger.info(s"$self announcing itself as the leader and sending ElectedMessage to neighbor ${processRecord.map(neighbor)}")
    processRecord.map(neighbor) ! ElectedMessage(uid)
  }
}