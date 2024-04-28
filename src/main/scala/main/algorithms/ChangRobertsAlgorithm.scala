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

/**
 * Object representing Chang Roberts algorithm.
 */
object ChangRobertsAlgorithm {
  // Logger for logging messages
  val logger = Logger(getClass.getName)

  /**
   * Main entry point of Chang Roberts algorithm.
   * Initializes the actor system, loads the topology from a application.conf , creates the actors,
   * and runs the algorithm.
   */
  def main(): Unit = {
    runAlgorithm()
  }

  // Method to run the Chang-Roberts algorithm
  def runAlgorithm(): Unit = {
    // Creating an Akka ActorSystem named "ChangRobertsSystem"
    val system = ActorSystem("ChangRobertsSystem")
    logger.info("ActorSystem ChangRobertsSystem Initialized!")

    // Creating a new ProcessRecord to store the mapping of process IDs to actor references
    val record = new ProcessRecord()

    // Getting the test data configuration from ApplicationProperties
    val testData = ApplicationProperties.changRobertsData

    // Creating the test data by initializing the processes and updating the ProcessRecord
    createTestData(testData, record, system)

    logger.info("Test Data Loaded")

    // Creating a Terminator actor to handle system termination
    val terminator = system.actorOf(Props(new Terminator(system)))
    record.map.put(-1, terminator)

    logger.info("Initiating the Algorithm")

    // Getting the total number of nodes in the system
    val numNodes = record.map.size

    // Randomly selecting the number of initiator processes (between 1 and numNodes)
    val numInitiators = Random.nextInt(numNodes) + 1

    // Randomly selecting the initiator process IDs
    val randomInitiators = Random.shuffle(record.map.keys.toList).take(numInitiators)

    // Sending the Initiate message to the selected initiator processes
    randomInitiators.foreach { initiatorId =>
      record.map(initiatorId) ! Initiate
    }

    // Waiting for the system to terminate (with a timeout of 30 seconds)
    Await.ready(system.whenTerminated, 30.seconds)
    logger.info("Terminating the algorithm")
  }

  // Method to create the test data by initializing the processes and updating the ProcessRecord
  def createTestData(data: Config, record: ProcessRecord, system: ActorSystem): Unit = {
    // Iterating over the node data configuration
    for (nodeData <- data.getConfigList("nodeData").asScala.toList) {
      // Extracting the node ID, UID, and neighbor ID from the configuration
      val id = nodeData.getInt("Node")
      val uid = nodeData.getInt("UID")
      val neighbor = nodeData.getInt("Neighbor")

      // Creating a new ChangRobertsProcess actor with the extracted data and adding it to the ActorSystem
      val process = system.actorOf(Props(new ChangRobertsProcess(id, uid, neighbor, system, record)), name = "process" + id)

      // Updating the ProcessRecord with the process ID and actor reference
      record.map.put(id, process)
    }
  }
}