package main.processes

import akka.actor.{Actor, ActorSystem}
import akka.event.slf4j.Logger
import main.utility._
import scala.math.max

// FranklinProcess class representing a process in the Franklin election algorithm
class FranklinProcess(id: Int, leftNeighbor: Int, rightNeighbor: Int, val system: ActorSystem, val processRecord: ProcessRecord) extends Actor {
  val logger = Logger(getClass.getName)
  var isActive = true // Flag indicating whether the process is active
  var leftId: Int = -1 // ID of the left neighbor
  var rightId: Int = -1 // ID of the right neighbor

  // Called before the actor starts processing messages
  override def preStart(): Unit = {
    super.preStart()
  }

  // Message handling logic
  def receive: Receive = {
    // Initiate message received
    case Initiate =>
      logger.info(s"Initiated Franklin Process $id")
      // If the process is active, request IDs from neighbors
      if (isActive) {
        processRecord.map.get(leftNeighbor).get ! LeftIdRequest
        processRecord.map.get(rightNeighbor).get ! RightIdRequest
      }

    // LeftIdRequest message received
    case LeftIdRequest =>
      // If the process is active, respond with its own ID; otherwise, forward the request to the left neighbor
      if (isActive) {
        sender() ! LeftIdResponse(Some(id))
      } else {
        processRecord.map.get(leftNeighbor).get ! LeftIdRequest
      }

    // RightIdRequest message received
    case RightIdRequest =>
      // If the process is active, respond with its own ID; otherwise, forward the request to the right neighbor
      if (isActive) {
        sender() ! RightIdResponse(Some(id))
      } else {
        processRecord.map.get(rightNeighbor).get ! RightIdRequest
      }

    // LeftIdResponse message received
    case LeftIdResponse(idOpt) =>
      // If the process is inactive, forward the response to the right neighbor; otherwise, compare IDs
      if (!isActive) {
        processRecord.map.get(rightNeighbor).get ! LeftIdResponse(idOpt)
      } else {
        leftId = idOpt.getOrElse(-1)
        compareIds()
      }

    // RightIdResponse message received
    case RightIdResponse(idOpt) =>
      // If the process is inactive, forward the response to the left neighbor; otherwise, compare IDs
      if (!isActive) {
        processRecord.map.get(leftNeighbor).get ! RightIdResponse(idOpt)
      } else {
        rightId = idOpt.getOrElse(-1)
        compareIds()
      }

    // TurnPassive message received
    case TurnPassive =>
      // Turn the process passive and acknowledge
      logger.info(s"Turning process $id passive")
      isActive = false
      sender() ! Ack
  }

  // Method to compare IDs and send a checkpoint message
  private def compareIds(): Unit = {
    // If IDs of both neighbors are received
    if (leftId != -1 && rightId != -1) {
      logger.info(s"Received neighbour IDs for process $id - Left: $leftId, Right: $rightId")
      // Determine if the process should stay active based on maximum value of IDs
      val stayActive: Boolean = max(max(id, leftId), rightId) == id
      // Send checkpoint message to the orchestrator
      processRecord.map.get(-2).get ! Checkpoint(id, stayActive, leftId == id)
      // Reset neighbor IDs
      leftId = -1
      rightId = -1
    }
  }
}
