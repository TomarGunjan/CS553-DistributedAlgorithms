package main.algorithms

import akka.actor.{ActorSystem, Props}
import akka.event.slf4j.Logger
import main.processes.EchoProcess
import main.utility.{MessageTypes, Terminator, ApplicationProperties, TopologyReader, AllProcessesCreated}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random

/**
 * Object representing the Echo algorithm.
 * Extends the MessageTypes trait to have access to the message types used in the algorithm.
 */
object EchoAlgorithm extends MessageTypes {
  val log = Logger(getClass.getName)

  /**
   * Main method to run the Echo algorithm.
   */
  def main(): Unit = {
    // Create an actor system for the Echo algorithm
    val system = ActorSystem("EchoAlgorithmSystem")

    // Read the process configuration from the input file specified in ApplicationProperties
    val filename = ApplicationProperties.echoInputFile
    val processConfig: Map[String, List[String]] = TopologyReader.readTopology(filename)

    // Get all process IDs from the configuration and convert them to integers
    val allProcessIds = processConfig.keys.map(_.toInt).toList

    // Randomly select an initiator process ID
    val randomInitiatorId = Random.shuffle(allProcessIds).head
    log.info(s"Randomly selected initiator ID: $randomInitiatorId")

    // Create EchoProcess actors based on the process configuration
    // Each process actor is created with its ID, list of neighbors, and initiator flag
    // The created actors are stored in a map with their ID as the key
    val processActors = processConfig.map { case (id, neighbors) =>
      val isInitiator = id.toInt == randomInitiatorId
      val processActor = system.actorOf(Props(new EchoProcess(id, neighbors, isInitiator)), id)
      log.info(s"Created EchoProcess actor for Process $id with neighbors: ${neighbors.mkString(", ")}, Actor: $processActor")
      (id.toInt, processActor)
    }.toMap

    // Send AllProcessesCreated message to the initiator process
    // This triggers the start of the Echo algorithm
    processActors(randomInitiatorId) ! AllProcessesCreated

    // Create a Terminator actor to handle the termination of the algorithm
    val terminator = system.actorOf(Props(new Terminator(system)), "terminator")

    // Wait for the algorithm to terminate or timeout after 30 seconds
    Await.ready(system.whenTerminated, 30.seconds)
    log.info("Algorithm terminated")
  }
}