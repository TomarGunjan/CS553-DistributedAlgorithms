package main.utility

import akka.actor.{Actor, ActorSystem}
import akka.event.slf4j.Logger

// Orchestrator class responsible for coordinating the Franklin Algorithm
class FranklinOrchestrator(var processes: List[Int], val system: ActorSystem, val processRecord: ProcessRecord) extends Actor {
  var roundProcessCount = 0 // Counter to track the number of processes that have sent checkpoint messages
  val log = Logger(getClass.getName) // Logger for logging messages
  var round: Int = -1 // Current round number
  var passiveProcess: List[Int] = List() // List of processes that are passive in the current round
  var passiveProcessCount = 0 // Counter to track the number of passive processes that have acknowledged

  // Called before the actor starts processing messages
  override def preStart(): Unit = {
    super.preStart()
  }

  // Message handling logic
  def receive: Receive = {
    case Initiate =>
      // Orchestrator initiated
      log.info("Orchestrator initiated")
      initiateRound()

    case Checkpoint(id, isActive, isLeader) =>
      // Checkpoint message received from a process
      roundProcessCount += 1
      log.info(s"Checkpoint message for Round $round received from process $id")

      // Add passive process to the list if it is not active
      if (!isActive) {
        log.info(s"Adding $id to passive processes")
        passiveProcess = id :: passiveProcess
      }

      // If all messages for the round are received
      if (roundProcessCount == processes.size) {
        log.info(s"All messages Received. Round $round ended.")

        // If the process is the leader, terminate the algorithm
        if (isLeader) {
          log.info(s"Process $id is the Leader. Terminating Algorithm")
          processRecord.map.get(-1).get ! TerminateSystem
        } else {
          // If there are no passive processes, initiate the next round
          if (passiveProcess.isEmpty) {
            initiateRound()
          } else {
            // If there are passive processes, turn them passive and remove from the list of active processes
            passiveProcess.foreach(proId => {
              processes = processes.filterNot(_ == proId)
              processRecord.map.get(proId).get ! TurnPassive
            })
          }
        }
      }

    case Ack =>
      // Acknowledgment message received from a passive process
      passiveProcessCount += 1
      // If all passive processes have acknowledged, initiate the next round
      if (passiveProcessCount == passiveProcess.size) {
        initiateRound()
      }
  }

  // Method to initiate the next round
  private def initiateRound(): Unit = {
    round += 1 // Increment the round number
    roundProcessCount = 0 // Reset round process count
    passiveProcessCount = 0 // Reset passive process count
    log.info(s"Initiating Round $round")
    // Send Initiate message to all processes
    processes.foreach(pid => {
      processRecord.map.get(pid).get ! Initiate
    })
  }
}
