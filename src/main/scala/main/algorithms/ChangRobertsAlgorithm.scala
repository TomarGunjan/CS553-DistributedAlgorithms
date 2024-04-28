package main.algorithms

import akka.actor.{ActorSystem, Props}
import akka.event.slf4j.Logger
import com.typesafe.config.Config
import main.processes.ChangRobertsProcess
import main.utility.{ApplicationProperties, Initiate, ProcessRecord, Terminator}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters.ListHasAsScala
import scala.language.postfixOps
import scala.util.Random

object ChangRobertsAlgorithm {
  val logger = Logger(getClass.getName)

  def main(): Unit = {
    runAlgorithm()
  }

  def runAlgorithm(): Unit = {
    val system = ActorSystem("ChangRobertsSystem")
    logger.info("ActorSystem ChangRobertsSystem Initialized!")

    val record = new ProcessRecord()

    val testData = ApplicationProperties.changRobertsData

    createTestData(testData, record, system)

    logger.info("Test Data Loaded")

    val terminator = system.actorOf(Props(new Terminator(system)))
    record.map.put(-1, terminator)

    logger.info("Initiating the Algorithm")
    val numNodes = record.map.size
    val numInitiators = Random.nextInt(numNodes) + 1
    val randomInitiators = Random.shuffle(record.map.keys.toList).take(numInitiators)
    randomInitiators.foreach { initiatorId =>
      record.map(initiatorId) ! Initiate
    }

    Await.ready(system.whenTerminated, 30.seconds)
    logger.info("Terminating the algorithm")
  }

  def createTestData(data: Config, record: ProcessRecord, system: ActorSystem): Unit = {
    for (nodeData <- data.getConfigList("nodeData").asScala.toList) {
      val id = nodeData.getInt("Node")
      val uid = nodeData.getInt("UID")
      val neighbor = nodeData.getInt("Neighbor")
      val process = system.actorOf(Props(new ChangRobertsProcess(id, uid, neighbor, system, record)), name = "process" + id)
      record.map.put(id, process)
    }
  }
}