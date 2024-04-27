package main.algorithms

import akka.actor.{Actor, ActorRef, ActorSystem, Props, UnboundedStash}
import akka.dispatch.UnboundedMessageQueueSemantics

import scala.concurrent.ExecutionContext.Implicits.global
import akka.event.slf4j.Logger
import main.processes.LaiYangProcess
import main.utility.{ApplicationProperties, InitiateSnapshotActors, InitiateSnapshotWithMessageCount, ProcessRecord, SendMessage, SystemSnapshot, TopologyReader}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.Source
import scala.util.Random

object LaiYangAlgorithm{

  var systemSnapshot: SystemSnapshot = null
  var processRecord: ProcessRecord = null
  var snapshotTaken = false
  var numberOfProcessesCompletingSnapshot = 0

  def init(processRecord: ProcessRecord) {
    systemSnapshot = new SystemSnapshot()
    this.processRecord = processRecord
  }

  def main(): Unit = {
    val log = Logger(getClass.getName)

    systemSnapshot = new SystemSnapshot()
    processRecord = new ProcessRecord()

    val system = ActorSystem("LaiYang")

    val filename = ApplicationProperties.snapshotInputFile
    val topologyLines = Source.fromFile(filename).getLines().toList

    val processConfig: Map[String, List[String]] = TopologyReader.readTopology(filename)

    processConfig.foreach { case (id, neighbors) =>
      val process = system.actorOf(Props(new LaiYangProcess(id.toInt, neighbors.distinct.map(_.toInt), false)), s"process$id")
      processRecord.map += (id.toInt -> process)
    }

    processRecord.map.foreach { case (key, value) =>
      value ! InitiateSnapshotActors
    }

    val keySeq = processRecord.map.keys.toSeq

    Thread.sleep(100*processRecord.map.size)

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

    numberOfProcessesCompletingSnapshot = 0
    snapshotTaken = false

    system.terminate()

    Await.ready(system.whenTerminated, ((1.7 * processRecord.map.size)).seconds)
    log.info("Algorithm terminated")
  }
}
