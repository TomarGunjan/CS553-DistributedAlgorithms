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

object ChandyLamportAlgorithm extends App {

  var systemSnapshot:SystemSnapshot = null
  var processRecord:ProcessRecord = null
  var snapshotTaken = false
  var numberOfProcessesCompletingSnapshot = 0

  def init(processRecord: ProcessRecord) {
    systemSnapshot = new SystemSnapshot()
    this.processRecord = processRecord
  }

  def main() {

    val log = Logger(getClass.getName)

    systemSnapshot = new SystemSnapshot()
    processRecord = new ProcessRecord()

      var NUMBER_OF_ITERATIONS = 5

    // INITIALIZE THE AKKA ACTOR SYSTEM
    val system = ActorSystem("ChandyLamportAlgorithm")

    // ASCII ART HERE
//    println()

    //  println("Initializing Actors...")
    //  Thread.sleep(500)
    //  println("...")
    //  Thread.sleep(500)
    //  println("Actors initialized.")
    //  println()

    // TODO: INSERT NETGAMESIM CODE HERE

    val filename = ApplicationProperties.snapshotInputFile
    val topologyLines = Source.fromFile(filename).getLines().toList

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

    if (TopologyReader.hasCycle(processConfig)) {
      log.error("The input topology contains a cycle. Terminating the algorithm.")
      system.terminate()
      return
    }

    processConfig.foreach { case (id, neighbors) =>
      val process = system.actorOf(Props(new ChandyLamportProcess(id.toInt, neighbors.map(_.toInt), false)), s"process$id")
      processRecord.map += (id.toInt -> process)
    }

    //  processRecord.map ++= Map(
    //    0 -> system.actorOf(Props(new ChandyLamportProcess(0, List(1, 2), false)), "process0"),
    //    1 -> system.actorOf(Props(new ChandyLamportProcess(1, List(0, 2), false)), "process1"),
    //    2 -> system.actorOf(Props(new ChandyLamportProcess(2, List(0, 1), false)), "process2")
    //  )

    val keySeq = processRecord.map.keys.toSeq

//    def sendMessages(numberOfIterations: Int): Unit = {
//      var numberOfIterationsMutable = numberOfIterations
//      while (numberOfIterationsMutable > 0) {
//        val sender = processRecord.map(keySeq(Random.nextInt(keySeq.length)))
//        val receiver = processRecord.map(keySeq(Random.nextInt(keySeq.length)))
//        //      while (receiver == sender) {
//        //        receiver = processRecord.map(keySeq(Random.nextInt(keySeq.length)))
//        //      }
//        val operation = if (Random.nextInt(2) == 1) "increment" else "decrement"
//        sender ! SendMessage(receiver, operation)
//        numberOfIterationsMutable = numberOfIterationsMutable - 1
//      }
//    }

//    system.scheduler.scheduleOnce((2 * processRecord.map.size).seconds) {
//      processRecord.map.foreach { case (key, value) =>
//        value ! Kill
//      }
//      system.terminate()
//    }
//
//    system.scheduler.scheduleOnce((1.5 * processRecord.map.size).seconds) {
//      log.info("TERMINATING ALGORITHM")
//      system.terminate()
//    }

//      sendMessages(5)

//      Thread.sleep(2000)

    val selectedProcess = processRecord.map(keySeq(Random.nextInt(keySeq.length)))
//    processRecord.map(1) ! SendMessage(processRecord.map(0), "increment")
    selectedProcess ! InitiateSnapshot(true)
//    processRecord.map(2) ! SendMessage(processRecord.map(1), "increment")
//    processRecord.map(3) ! SendMessage(processRecord.map(2), "decrement")

//      sendMessages(5)

    while (numberOfProcessesCompletingSnapshot < processRecord.map.size) {
//      NUMBER_OF_ITERATIONS = NUMBER_OF_ITERATIONS - 1
      val selectedKey = Random.nextInt(3)
      //    println("selectedKey " + selectedKey)
      if (selectedKey == 0 && !snapshotTaken) {
        snapshotTaken = true
        val selectedProcess = processRecord.map(keySeq(Random.nextInt(keySeq.length)))
        selectedProcess ! InitiateSnapshot(true)
//        sendMessages(5)
      } else if (selectedKey == 1 || selectedKey == 2) {
        val selectedId = keySeq(Random.nextInt(keySeq.length))
        val sender = processRecord.map(selectedId)
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

//    log.info("Exiting while loop")

//    try {
//      Await.ready(system.whenTerminated, 1*(processRecord.map.size).seconds)
//    } catch {
//      case e: Exception =>
//        log.info("Algorithm terminated")
//        system.terminate()
//    }

    Await.ready(system.whenTerminated, ((1.7 * processRecord.map.size)).seconds)
    log.info("Algorithm terminated")
  }

  main()


//  val terminator = system.actorOf(Props(new Terminator(system)), "terminator")
//  processRecord.map.put(-1, terminator)
//
//  log.info("Sending initial Wave messages")
//  val leafNodes = processConfig.filter { case (_, neighbors) => neighbors.size == 1 }.keys
//  leafNodes.foreach(id => processRecord.map(id) ! Wave)
//
//  Await.ready(system.whenTerminated, 30.seconds)
//  log.info("Algorithm terminated")

//  private val process0 = system.actorOf(Props(new ChandyLamportProcess(0, ListBuffer(1, 2), false)), "process_0")
//  private val process1 = system.actorOf(Props(new ChandyLamportProcess(1, ListBuffer(0, 2), false)), "process_1")
//  private val process2 = system.actorOf(Props(new ChandyLamportProcess(2, ListBuffer(0, 1), false)), "process_2")
//
//  ProcessRecord.map ++= Map(
//    0 -> process0,
//    1 -> process1,
//    2 -> process2
//  )
//
//  Thread.sleep(1000)
//
//  println("Initiating Snapshot...")
//  Thread.sleep(500)
//  println("...")
////  Thread.sleep(500)
//
//  private val selectedKey = 0 + Random.nextInt((2 - 0) + 1)
//
//  private val initiatorProcess = ProcessRecord.map(selectedKey)
//
//  initiatorProcess ! InitiateSnapshot
//  initiatorProcess ! SendMessage(process1, "increment")
//  process1 ! SendMessage(process2, "decrement")
//  process1 ! SendMessage(process2, "increment")
//  process2 ! SendMessage(process1, "increment")
//
//  Thread.sleep(10000)
//
//  system.terminate()

}
