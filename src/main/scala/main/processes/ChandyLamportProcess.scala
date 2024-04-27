package main.processes

import akka.actor.{Actor, ActorRef}
import akka.event.slf4j.Logger
import main.algorithms.ChandyLamportAlgorithm.{numberOfProcessesCompletingSnapshot, processRecord, systemSnapshot}
import main.utility.{InitiateSnapshot, MessageRecord, PerformAction, ProcessSnapshotData, SendMessage}

import scala.collection.mutable.ListBuffer
import scala.util.Random

class ChandyLamportProcess(val id: Int, val neighbors: List[Int], var snapshotTaken: Boolean) extends Actor{

  private var storedVariable = 0;
  private var markersReceived = 0;
  // A LIST THAT KEEPS TRACK OF WHAT CHANNELS TO STOP TRACKING ON
  private val stopRecordingOn = ListBuffer[ActorRef]()

  var log = Logger(getClass.getName)

  override def postStop(): Unit = {
    log.info(s"SHUTDOWN: process${id} is being stopped.")
  }

  override def receive: Receive = {
    case InitiateSnapshot(start) =>
      // THIS CASE IS PROCESSED WHEN AN ACTOR INITIALIZES THE SNAPSHOT OR RECEIVES A MARKER MESSAGE
      // start WILL BE true WHEN the actor initiates the snapshot, false OTHERWISE (false MEANS MARKER MESSAGE)
      if(!snapshotTaken) {
        // INITIATE SNAPSHOT PROCEDURE AND SEND MARKERS TO NEIGHBORS
        log.info(s"Snapshot initiated by ${self.path.name}")
        systemSnapshot.systemSnapshot += (id -> new ProcessSnapshotData(storedVariable, ListBuffer()))
        snapshotTaken = !snapshotTaken

        if(!start) {
          markersReceived += 1
        }

        neighbors.foreach(neighbor => processRecord.map(neighbor) ! InitiateSnapshot(false))
      } else {
        // IF SNAPSHOT ALREADY TAKEN AND MARKER RECEIVED FROM A NEIGHBOR, STOP RECORDING ON THE CHANNEL TO THE NEIGHBOR
        markersReceived += 1
        log.info(s"Recording stopped on channel ${sender.path.name} --> ${self.path.name}")
        stopRecordingOn += sender
      }

      // IF MARKER HAS BEEN RECEIVED FROM ALL NEIGHBORS, SAVE THE SNAPSHOT AND RESET VARIABLES
      if (markersReceived == neighbors.size) {
        snapshotTaken = !snapshotTaken
        markersReceived = 0
        stopRecordingOn.clear()
        systemSnapshot.printSnapshotData(id)
        numberOfProcessesCompletingSnapshot += 1
      }
    case SendMessage(messageBody) =>
      // PICKS A RANDOM NEIGHBOR AND SENDS AND INCREMENT OR DECREMENT MESSAGE
      processRecord.map(neighbors(Random.nextInt(neighbors.length))) ! PerformAction(messageBody)
    case PerformAction(action: String) =>
      // EXECUTES THE INCREMENT/DECREMENT OPERATION ON THE STORED VARIABLE
//      val senderName = if(sender == context.system.deadLetters) self.path.name else sender.path.name

      if(action.equals("Increment")) {
        storedVariable += 1;
//        log.info(s"Increment message sent from ${senderName}... value of storedVariable in ${self.path.name} is ${storedVariable}")
      } else if(action.equals("decrement")) {
        storedVariable -= 1;
//        log.info(s"Decrement message sent from ${senderName}... value of storedVariable in ${self.path.name} is ${storedVariable}")
      }

      if (this.snapshotTaken && !stopRecordingOn.contains(sender)) {
        systemSnapshot.systemSnapshot(id).messageQueue += new MessageRecord(sender, action)
      }

  }
}
