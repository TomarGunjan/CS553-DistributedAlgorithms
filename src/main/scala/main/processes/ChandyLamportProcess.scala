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
  private val stopRecordingOn = ListBuffer[ActorRef]()

  var log = Logger(getClass.getName)

  override def postStop(): Unit = {
    log.info(s"SHUTDOWN: process${id} is being stopped.")
  }

  override def receive: Receive = {
    case InitiateSnapshot(start) =>
//      Thread.sleep(1000)
      if(!snapshotTaken) {
        log.info(s"Snapshot initiated by ${self.path.name}")
        systemSnapshot.systemSnapshot += (id -> new ProcessSnapshotData(storedVariable, ListBuffer()))
        snapshotTaken = !snapshotTaken

        if(!start) {
          markersReceived += 1
        }

//        markersReceived += 1

        neighbors.foreach(neighbor => processRecord.map(neighbor) ! InitiateSnapshot(false))
      } else {
        markersReceived += 1
        log.info(s"Recording stopped on channel ${sender.path.name} --> ${self.path.name}")
        stopRecordingOn += sender
      }

      if (markersReceived == neighbors.size) {
        snapshotTaken = !snapshotTaken
        markersReceived = 0
        stopRecordingOn.clear()
        //          log.info(s"Snapshot procedure ended for ${self.path.name}\n")
        //          log.info(s"Saved Snapshot for ${self.path.name} -->")
        systemSnapshot.printSnapshotData(id)
        numberOfProcessesCompletingSnapshot += 1
//        context.stop(self)
      }
    case SendMessage(messageBody) =>
//      Thread.sleep(1000)
//      if(target == context.system.deadLetters) {
//        self ! PerformAction(messageBody)
//      }
      processRecord.map(neighbors(Random.nextInt(neighbors.length))) ! PerformAction(messageBody)
//      processRecord.map(neighbors.head) ! PerformAction(messageBody)
    case PerformAction(action: String) =>
//      Thread.sleep(1000)

      val senderName = if(sender == context.system.deadLetters) self.path.name else sender.path.name

      if(action.equals("Increment")) {
        storedVariable += 1;
//        log.info(s"Increment message sent from ${senderName}... value of storedVariable in ${self.path.name} is ${storedVariable}")
      } else if(action.equals("decrement")) {
        storedVariable -= 1;
//        log.info(s"Decrement message sent from ${senderName}... value of storedVariable in ${self.path.name} is ${storedVariable}")
      }

//      if (this.snapshotTaken && !stopRecordingOn.contains(sender)) {
//        systemSnapshot.systemSnapshot(id).messageQueue += new MessageRecord(sender, action)
//      }

      if (this.snapshotTaken && !stopRecordingOn.contains(sender)) {
        systemSnapshot.systemSnapshot(id).messageQueue += new MessageRecord(sender, action)
      }

//      if(sender == context.system.deadLetters) {
//        if (this.snapshotTaken && !stopRecordingOn.contains(self)) {
//          systemSnapshot.systemSnapshot(id).messageQueue += new MessageRecord(self, action)
//        }
//      } else {
////        if (this.snapshotTaken && !stopRecordingOn.contains(sender)) {
////          systemSnapshot.systemSnapshot(id).messageQueue += new MessageRecord(sender, action)
////        }
//      }
  }
}
