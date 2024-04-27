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

object LaiYangAlgorithm extends App{

  var systemSnapshot: SystemSnapshot = null
  var processRecord: ProcessRecord = null
  var snapshotTaken = false
  var numberOfProcessesCompletingSnapshot = 0

//  val system = ActorSystem("LaiYang")
//  var log = Logger(getClass.getName)
//
//  val systemSnapshot = new SystemSnapshot()
//  val processRecord = new ProcessRecord()
//
//  var snapshotTaken = false
//  var numberOfProcessesCompletingSnapshot = 0

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
//
//    val processConfig: Map[Int, List[Int]] = topologyLines
//      .filter(line => line.contains("->"))
//      .flatMap { line =>
//        val parts = line.split("->")
//        if (parts.length == 2) {
//          val from = parts(0).trim.replaceAll("\"", "").toInt
//          val to = parts(1).split("\\[")(0).trim.replaceAll("\"", "").toInt
//          List((from, to), (to, from))
//        } else {
//          List.empty
//        }
//      }
//      .groupBy(_._1)
//      .mapValues(_.map(_._2).toList)
//      .toMap

    val processConfig: Map[String, List[String]] = TopologyReader.readTopology(filename)

//    if (TopologyReader.hasCycle(processConfig)) {
//      log.error("The input topology contains a cycle. Terminating the algorithm.")
//      system.terminate()
//      return
//    }

    processConfig.foreach { case (id, neighbors) =>
      val process = system.actorOf(Props(new LaiYangProcess(id.toInt, neighbors.distinct.map(_.toInt), false)), s"process$id")
      processRecord.map += (id.toInt -> process)
    }

//    processConfig.foreach { case (id, neighbors) =>
//      val process = system.actorOf(Props(new LaiYangProcess(id, neighbors.distinct, false)), s"process$id")
//      processRecord.map += (id -> process)
//    }

    processRecord.map.foreach { case (key, value) =>
      value ! InitiateSnapshotActors
    }

    val keySeq = processRecord.map.keys.toSeq

    Thread.sleep(100*processRecord.map.size)

    while (numberOfProcessesCompletingSnapshot < processRecord.map.size) {
      //      NUMBER_OF_ITERATIONS = NUMBER_OF_ITERATIONS - 1
      val selectedKey = Random.nextInt(3)
      //    println("selectedKey " + selectedKey)
      if (selectedKey == 0 && !snapshotTaken) {
        snapshotTaken = true
        val selectedProcess = processRecord.map(keySeq(Random.nextInt(keySeq.length)))
        selectedProcess ! InitiateSnapshotWithMessageCount(-1, start = true)
        //        sendMessages(5)
      } else if (selectedKey == 1 || selectedKey == 2) {
        val sender = processRecord.map(keySeq(Random.nextInt(keySeq.length)))
        //      val receiver = processRecord.map(keySeq(Random.nextInt(keySeq.length)))
        //      while(receiver == sender) {
        //        receiver = processRecord.map(keySeq(Random.nextInt(keySeq.length)))
        //      }
        val operation = if (selectedKey == 1) "increment" else "decrement"
        sender ! SendMessage(operation)

      }
      //      Thread.sleep(100)
    }

    numberOfProcessesCompletingSnapshot = 0
    snapshotTaken = false

    system.terminate()

    Await.ready(system.whenTerminated, ((1.7 * processRecord.map.size)).seconds)
    log.info("Algorithm terminated")
  }

  // TODO: INSERT NETGAMESIM CODE HERE

//  val filename = ApplicationProperties.snapshotInputFile
//  val topologyLines = Source.fromFile(filename).getLines().toList
//
//  val processConfig: Map[Int, List[Int]] = topologyLines
//    .filter(line => line.contains("->"))
//    .flatMap { line =>
//      val parts = line.split("->")
//      if (parts.length == 2) {
//        val from = parts(0).trim.replaceAll("\"", "").toInt
//        val to = parts(1).split("\\[")(0).trim.replaceAll("\"", "").toInt
//        List((from, to), (to, from))
//      } else {
//        List.empty
//      }
//    }
//    .groupBy(_._1)
//    .mapValues(_.map(_._2).toList)
//    .toMap
//
//  processConfig.foreach { case (id, neighbors) =>
//    val process = system.actorOf(Props(new LaiYangProcess(id, neighbors, false)), s"process$id")
//    processRecord.map += (id -> process)
//  }

//  val processConfig = Map(
//    0 -> List(1, 2),
//    1 -> List(0, 2),
//    2 -> List(0, 1)
//  )
//
//  private val process0 = system.actorOf(Props(new LaiYangProcess(0, List(1, 2), false)), "process_0")
//  private val process1 = system.actorOf(Props(new LaiYangProcess(1, List(0, 2), false)), "process_1")
//  private val process2 = system.actorOf(Props(new LaiYangProcess(2, List(0, 1), false)), "process_2")
//
//  processRecord.map ++= Map(
//    0 -> process0,
//    1 -> process1,
//    2 -> process2
//  )

//  processRecord.map.foreach { case (key, value) =>
//    value ! InitiateSnapshotActors
//  }
//
//  val keySeq = processRecord.map.keys.toSeq

//  Thread.sleep(1000);
//
//  println("Initiating Snapshot...")
//  Thread.sleep(500)
//  println("...")
  //  Thread.sleep(500)

//  Thread.sleep(100*processRecord.map.size)

//  while (numberOfProcessesCompletingSnapshot < processRecord.map.size) {
//    //      NUMBER_OF_ITERATIONS = NUMBER_OF_ITERATIONS - 1
//    val selectedKey = Random.nextInt(3)
//    //    println("selectedKey " + selectedKey)
//    if (selectedKey == 0 && !snapshotTaken) {
//      snapshotTaken = true
//      val selectedProcess = processRecord.map(keySeq(Random.nextInt(keySeq.length)))
//      selectedProcess ! InitiateSnapshotWithMessageCount(-1, start = true)
//      //        sendMessages(5)
//    } else if (selectedKey == 1 || selectedKey == 2) {
//      val sender = processRecord.map(keySeq(Random.nextInt(keySeq.length)))
////      val receiver = processRecord.map(keySeq(Random.nextInt(keySeq.length)))
//      //      while(receiver == sender) {
//      //        receiver = processRecord.map(keySeq(Random.nextInt(keySeq.length)))
//      //      }
//      val operation = if (selectedKey == 1) "increment" else "decrement"
//      sender ! SendMessage(operation)
//
//    }
//    //      Thread.sleep(100)
//  }

//  val keySeq = processRecord.map.keys.toSeq
//  var snapshotTaken = false
//
//  def sendMessages(numberOfIterations: Int): Unit = {
//    var numberOfIterationsMutable = numberOfIterations
//    while (numberOfIterationsMutable > 0) {
//      val sender = processRecord.map(keySeq(Random.nextInt(keySeq.length)))
//      val receiver = processRecord.map(keySeq(Random.nextInt(keySeq.length)))
//      //      while (receiver == sender) {
//      //        receiver = processRecord.map(keySeq(Random.nextInt(keySeq.length)))
//      //      }
//      val operation = if (Random.nextInt(2) == 1) "increment" else "decrement"
//      sender ! SendMessage(receiver, operation)
//      numberOfIterationsMutable = numberOfIterationsMutable - 1
//    }
//  }
//
//  system.scheduler.scheduleOnce((1.5 * processRecord.map.size).seconds) {
//    log.info("TERMINATING ALGORITHM")
//    system.terminate()
//  }
//
//  sendMessages(5)
//
//  val selectedProcess = processRecord.map(keySeq(Random.nextInt(keySeq.length)))
//  selectedProcess ! InitiateSnapshotWithMessageCount(-1, start = true)
//
//  sendMessages(5)


//  private val selectedKey = 0 + Random.nextInt((2 - 0) + 1)
//
//  private val initiatorProcess = processRecord.map(selectedKey)
//
//  initiatorProcess ! InitiateSnapshotWithMessageCount(-1, start = true)
//  initiatorProcess ! SendMessage(process1, "increment")
//  process1 ! SendMessage(process2, "decrement")
//  process1 ! SendMessage(process2, "increment")
//  process2 ! SendMessage(process1, "increment")

//  Thread.sleep(10000)
//
//  system.terminate()

}
