package main.algorithms

import akka.actor.{ActorSystem, Props}
import akka.event.slf4j.Logger
import main.processes.ChandyLamportProcess
import main.utility.{ApplicationProperties, InitiateSnapshot, ProcessRecord, SendMessage, SystemSnapshot, TopologyReader}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import scala.concurrent.duration._
import scala.util.Random

object ChandyLamportAlgorithm {

  var systemSnapshot:SystemSnapshot = null
  var processRecord:ProcessRecord = null
  var snapshotTaken = false
  var numberOfProcessesCompletingSnapshot = 0

  // THIS METHOD IS USED TO INITIALIZE THESE VARIABLES FOR RUNNING TEST CASES
  def init(processRecord: ProcessRecord) {
    systemSnapshot = new SystemSnapshot()
    this.processRecord = processRecord
  }

  def main() {

    val log = Logger(getClass.getName)

    systemSnapshot = new SystemSnapshot()
    processRecord = new ProcessRecord()

    // INITIALIZE THE AKKA ACTOR SYSTEM
    val system = ActorSystem("ChandyLamportAlgorithm")

    // READING THE GRAPH FROM THE DOT FILES AND SPAWING ACTORS
    val filename = ApplicationProperties.snapshotInputFile
    val topologyLines = Source.fromFile(filename).getLines().toList

    val processConfig: Map[String, List[String]] = TopologyReader.readTopology(filename)

    processConfig.foreach { case (id, neighbors) =>
      val process = system.actorOf(Props(new ChandyLamportProcess(id.toInt, neighbors.map(_.toInt), false)), s"process$id")
      processRecord.map += (id.toInt -> process)
    }

    val keySeq = processRecord.map.keys.toSeq

    // A WHILE LOOP TO SIMULATE THE WORKING OF A DISTRIBUTED SYSTEM
    while (numberOfProcessesCompletingSnapshot < processRecord.map.size) {
      // AN ACTION WILL BE PERFORMED BY A RANDOMLY SELECTED PROCESS
      val selectedKey = Random.nextInt(3)
      if (selectedKey == 0 && !snapshotTaken) {
        snapshotTaken = true
        val selectedProcess = processRecord.map(keySeq(Random.nextInt(keySeq.length)))
        selectedProcess ! InitiateSnapshot(true)
      } else if (selectedKey == 1 || selectedKey == 2) {
        val selectedId = keySeq(Random.nextInt(keySeq.length))
        val sender = processRecord.map(selectedId)
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
