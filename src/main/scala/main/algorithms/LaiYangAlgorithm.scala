package main.algorithms

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.event.slf4j.Logger
import main.processes.LaiYangProcess
import main.utility._

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.Source
import scala.util.Random

object LaiYangAlgorithm {

  var systemSnapshot: SystemSnapshot = null
  var processRecord: ProcessRecord = null
  var snapshotTaken = false
  var numberOfProcessesCompletingSnapshot = 0

  // THIS OBJECT IS USED BY LAI-YANG ACTORS TO KEEP TRACK OF THE NUMBER OF MESSAGES RECEIVED AND SEND TO EACH NEIGHBOR
  object MessageCountStore {
    val preSnapshotMessagesSent: mutable.Map[ActorRef, mutable.Map[ActorRef, Int]] = mutable.Map.empty
    val preSnapshotMessagesReceived: mutable.Map[ActorRef, mutable.Map[ActorRef, Int]] = mutable.Map.empty
  }

  // INIT UTILITY FUNCTION USED FOR TESTING PURPOSES
  def init(processRecord: ProcessRecord) {
    systemSnapshot = new SystemSnapshot()
    this.processRecord = processRecord
  }

  def main(): Unit = {
    val log = Logger(getClass.getName)

    systemSnapshot = new SystemSnapshot()
    processRecord = new ProcessRecord()

    val system = ActorSystem("LaiYang")

    // Read the process configuration from the input file specified in ApplicationProperties
    val filename = ApplicationProperties.snapshotInputFile
    val topologyLines = Source.fromFile(filename).getLines().toList

    // Create LaiYangProcess actors based on the process configuration
    // Each process actor is created with its ID, list of neighbors, and a boolean that indicates if snapshot was taken
    // The created actors are stored in a map with their ID as the key
    val processConfig: Map[String, List[String]] = TopologyReader.readTopology(filename)

    processConfig.foreach { case (id, neighbors) =>
      val process = system.actorOf(Props(new LaiYangProcess(id.toInt, neighbors.distinct.map(_.toInt), false)), s"process$id")
      log.info(s"process$id created")
      processRecord.map += (id.toInt -> process)
    }

    // inserting key-value pairs in MessageCountStore maps
    processConfig.foreach { case (id, neighbors) =>
      val process = processRecord.map(id.toInt)

      val preSnapshotMessagesSent: mutable.Map[ActorRef, Int] = mutable.Map.empty
      neighbors.distinct.foreach { key =>
        preSnapshotMessagesSent += (processRecord.map(key.toInt) -> 0)
      }

      MessageCountStore.preSnapshotMessagesSent += (process -> preSnapshotMessagesSent)

      val preSnapshotMessagesReceived: mutable.Map[ActorRef, Int] = mutable.Map.empty
      neighbors.distinct.foreach { key =>
        preSnapshotMessagesReceived += (processRecord.map(key.toInt) -> 0)
      }

      MessageCountStore.preSnapshotMessagesReceived += (process -> preSnapshotMessagesReceived)

    }


    val keySeq = processRecord.map.keys.toSeq

    // A WHILE LOOP TO SIMULATE THE WORKING OF A DISTRIBUTED SYSTEM
    while (numberOfProcessesCompletingSnapshot < processRecord.map.size) {
      val selectedKey = Random.nextInt(3)
      if (selectedKey == 0 && !snapshotTaken) {
        snapshotTaken = true
        val selectedProcess = processRecord.map(keySeq(Random.nextInt(keySeq.length)))
        selectedProcess ! InitiateSnapshotWithMessageCount(-1, start = true)
      } else if (selectedKey == 1 || selectedKey == 2) {
        val sender = processRecord.map(keySeq(Random.nextInt(keySeq.length)))
        val operation = if (selectedKey == 1) "increment" else "decrement"
        sender ! SendMessage(operation)
      }
    }

    // RESETTING AFTER THE ALGORITHM HAS TERMINATED
    numberOfProcessesCompletingSnapshot = 0
    snapshotTaken = false

    system.terminate()

    Await.ready(system.whenTerminated, ((1.7 * processRecord.map.size)).seconds)
    log.info("Algorithm terminated")
  }
}
