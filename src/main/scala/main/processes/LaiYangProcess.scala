package main.processes

import akka.actor.{Actor, ActorRef, UnboundedStash}
import akka.dispatch.UnboundedMessageQueueSemantics
import akka.event.slf4j.Logger
import main.algorithms.LaiYangAlgorithm.{numberOfProcessesCompletingSnapshot, processRecord, systemSnapshot}
import main.utility.{InitiateSnapshotActors, InitiateSnapshotWithMessageCount, MessageRecord, PerformActionWithTagPayload, ProcessSnapshotData, SendMessage}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Random

class LaiYangProcess(val id: Int, val neighbors: List[Int], var snapshotTaken: Boolean) extends Actor with UnboundedStash with UnboundedMessageQueueSemantics {

  var log = Logger(getClass.getName)

  private var storedVariable = 0
  private var incomingChannelsRecorded = 0
  private val preSnapshotMessagesSent: mutable.Map[ActorRef, Int] = mutable.Map.empty
  private val preSnapshotMessagesToReceive: mutable.Map[ActorRef, Int] = mutable.Map.empty

  def init(): Unit = {
    neighbors.foreach { key =>
      preSnapshotMessagesSent += (processRecord.map(key) -> 0)
    }

    neighbors.foreach { key =>
      preSnapshotMessagesToReceive += (processRecord.map(key) -> 0)
    }
  }

//  override def postStop(): Unit = {
//    // Cleanup tasks or finalization logic
//    println(s"Actor${id} is being stopped. Performing cleanup tasks...")
//    // Release resources, close connections, etc.
//  }

  override def postStop(): Unit = {
    log.info(s"SHUTDOWN: process${id} is being stopped.")
  }

  def receive: Receive = {
    case InitiateSnapshotActors =>
      log.info(s"process${id} being initialized")
      init()
    case InitiateSnapshotWithMessageCount(n, start) =>
//      Thread.sleep(1000)
      if (!start) {
//        val realSender = if (sender == context.system.deadLetters) self else sender
        val messagesToReceive = n + preSnapshotMessagesToReceive(sender)
        if (messagesToReceive == 0) {
          incomingChannelsRecorded += 1
          log.info(s"Recording stopped on channel ${sender.path.name} --> ${self.path.name}")
        }
        preSnapshotMessagesToReceive += (sender -> messagesToReceive)
      }

      if (!this.snapshotTaken) {
        log.info(s"Snapshot initiated by ${self.path.name}")
        snapshotTaken = !snapshotTaken
        systemSnapshot.systemSnapshot += (id -> new ProcessSnapshotData(storedVariable, ListBuffer()))
//        if (sender != context.system.deadLetters) {
//          val messagesToReceive = n + preSnapshotMessagesToReceive(sender)
//          if (messagesToReceive == 0) {
//            incomingChannelsRecorded += 1
//            log.info(s"Recording stopped on channel ${sender.path.name} --> ${self.path.name}")
//          }
//          preSnapshotMessagesToReceive += (sender -> messagesToReceive)
//        }
        neighbors.foreach(neighbor => processRecord.map(neighbor) ! InitiateSnapshotWithMessageCount(preSnapshotMessagesSent(processRecord.map(neighbor)), start = false))
      }
//      else {
//        if (sender != context.system.deadLetters) {
//          val messagesToReceive = n + preSnapshotMessagesToReceive(sender)
//          if (messagesToReceive == 0) {
//            incomingChannelsRecorded += 1
//            log.info(s"Recording stopped on channel ${sender.path.name} --> ${self.path.name}")
//          }
//          preSnapshotMessagesToReceive += (sender -> messagesToReceive)
//        }
//      }

      if (incomingChannelsRecorded == neighbors.size) {
        snapshotTaken = !snapshotTaken
        log.info(s"Snapshot procedure ended for ${self.path.name}")
        log.info(s"Saved Snapshot for ${self.path.name} -->")
        systemSnapshot.printSnapshotData(id)
        numberOfProcessesCompletingSnapshot += 1
      }

    case SendMessage(str) =>
//      Thread.sleep(1000)
//      processRecord.map(Random.nextInt(neighbors.length)) ! PerformAction(messageBody)
        val receiver = processRecord.map(neighbors(Random.nextInt(neighbors.length)))
      if (!snapshotTaken) preSnapshotMessagesSent += (receiver -> (preSnapshotMessagesSent(receiver) + 1))
      receiver ! PerformActionWithTagPayload(str, snapshotTaken)
    case PerformActionWithTagPayload(action, tag) =>
//      Thread.sleep(1000)
      if (action.equals("Increment")) {
        storedVariable += 1;
//        log.info(s"Increment message sent from ${sender.path.name}... value of storedVariable in ${self.path.name} is ${storedVariable}")
      } else if (action.equals("decrement")) {
        storedVariable -= 1;
//        log.info(s"Decrement message sent from ${sender.path.name}... value of storedVariable in ${self.path.name} is ${storedVariable}")
      }
      if (!tag) {
        preSnapshotMessagesToReceive += (sender -> (preSnapshotMessagesToReceive(sender) - 1))
        if (this.snapshotTaken) {
          systemSnapshot.systemSnapshot(id).messageQueue += new MessageRecord(sender, action)
        }
      } else if (tag && !this.snapshotTaken && this.incomingChannelsRecorded < neighbors.size) {
        self ! InitiateSnapshotWithMessageCount(-1, start = true)
      }
  }
}
