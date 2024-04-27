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

  // TO STORE HOW MANY MESSAGES SENT/RECEIVED TO/FROM EACH NEIGHBOR
  private val preSnapshotMessagesSent: mutable.Map[ActorRef, Int] = mutable.Map.empty
  private val preSnapshotMessagesToReceive: mutable.Map[ActorRef, Int] = mutable.Map.empty

  // INITIALIZES THE MAPS WITH KEY-VALUE PAIRS
  def init(): Unit = {
    neighbors.foreach { key =>
      preSnapshotMessagesSent += (processRecord.map(key) -> 0)
    }

    neighbors.foreach { key =>
      preSnapshotMessagesToReceive += (processRecord.map(key) -> 0)
    }
  }

  override def postStop(): Unit = {
    log.info(s"SHUTDOWN: process${id} is being stopped.")
  }

  def receive: Receive = {
    case InitiateSnapshotActors =>
      // THIS RUNS THE init() METHOD
      log.info(s"process${id} being initialized")
      init()
    case InitiateSnapshotWithMessageCount(n, start) =>
      // start = false IMPLIES A CONTROL MESSAGE true MEANS THE PROCESS HAS INITIATED THE SNAPSHOT PROCESS BY ITSELF
      if (!start) {
        // IF ALL PRE-SNAPSHOT MESSAGES HAVE BEEN RECEIVED FROM THE CHANNEL, WE STOP RECORDING
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
        neighbors.foreach(neighbor => processRecord.map(neighbor) ! InitiateSnapshotWithMessageCount(preSnapshotMessagesSent(processRecord.map(neighbor)), start = false))
      }

      // IF ALL INCOMING CHANNELS HAVE BEEN RECORDED PRINT THE SNAPSHOT DATA
      if (incomingChannelsRecorded == neighbors.size) {
        snapshotTaken = !snapshotTaken
        log.info(s"Snapshot procedure ended for ${self.path.name}")
        log.info(s"Saved Snapshot for ${self.path.name} -->")
        systemSnapshot.printSnapshotData(id)
        numberOfProcessesCompletingSnapshot += 1
      }

    // SENDS AN INCREMENT OR DECREMENT MESSAGE TO A RANDOM NEIGHBOR WITH A TAG
    case SendMessage(str) =>
      val receiver = processRecord.map(neighbors(Random.nextInt(neighbors.length)))
      if (!snapshotTaken) preSnapshotMessagesSent += (receiver -> (preSnapshotMessagesSent(receiver) + 1))
      receiver ! PerformActionWithTagPayload(str, snapshotTaken)
    case PerformActionWithTagPayload(action, tag) =>
      // PERFORM INCREMENT OR DECREMENT ON storedVariable DEPENDING ON
      if (action.equals("Increment")) {
        storedVariable += 1;
//        log.info(s"Increment message sent from ${sender.path.name}... value of storedVariable in ${self.path.name} is ${storedVariable}")
      } else if (action.equals("decrement")) {
        storedVariable -= 1;
//        log.info(s"Decrement message sent from ${sender.path.name}... value of storedVariable in ${self.path.name} is ${storedVariable}")
      }

      // IF TAG IS FALSE, WE CHECK IF WE HAVE RECEIVED ALL FALSE MESSAGES FROM THE CHANNEL, AND STOP RECORDING IF WE HAVE
      if (!tag) {
        preSnapshotMessagesToReceive += (sender -> (preSnapshotMessagesToReceive(sender) - 1))
        if (this.snapshotTaken) {
          val received = preSnapshotMessagesToReceive(sender)
          if (received == 0) {
            incomingChannelsRecorded += 1
            log.info(s"Recording stopped on channel ${sender.path.name} --> ${self.path.name}")
          }

          if (incomingChannelsRecorded == neighbors.size) {
            snapshotTaken = !snapshotTaken
            log.info(s"Snapshot procedure ended for ${self.path.name}")
            log.info(s"Saved Snapshot for ${self.path.name} -->")
            systemSnapshot.printSnapshotData(id)
            numberOfProcessesCompletingSnapshot += 1
          }
          systemSnapshot.systemSnapshot(id).messageQueue += new MessageRecord(sender, action)
        }
      } else if (tag && !this.snapshotTaken && this.incomingChannelsRecorded < neighbors.size) {
        // IF MESSAGE IS TAGGED WITH TRUE AND THE PROCESS HAS NOT TAKEN SNAPSHOT, IT INITIATES ITS OWN SNAPSHOT PROCEDURE
        self ! InitiateSnapshotWithMessageCount(-1, start = true)
      }
  }
}
