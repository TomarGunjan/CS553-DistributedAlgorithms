package main.algorithms

import akka.actor.{ActorSystem, Props}
import akka.event.slf4j.Logger
import main.processes.EchoProcess
import main.utility.{MessageTypes, Terminator, ApplicationProperties, TopologyReader, AllProcessesCreated, ProcessRecord}

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
   * Main entry point of the Echo algorithm.
   * Initializes the actor system, loads the topology from a file, creates the actors,
   * and runs the algorithm.
   */
  def main(): Unit = {
    // Create an actor system for the Echo algorithm
    val system = ActorSystem("EchoAlgorithmSystem")
    // Create a ProcessRecord to store the mapping of process IDs to actor references
    val processRecord = new ProcessRecord

    // Load the topology file path from the application configuration
    val filename = ApplicationProperties.echoInputFile
    val processConfig: Map[Int, List[Int]] = TopologyReader.readTopology(filename).map {
      case (id, neighbors) => (id.toInt, neighbors.map(_.toInt))
    }

    // Get all process IDs
    val allProcessIds = processConfig.keys.toList
    // Randomly select the initiator process ID from the list of process IDs
    val randomInitiatorId = Random.shuffle(allProcessIds).head
    log.info(s"Randomly selected initiator ID: $randomInitiatorId")

    // Create EchoProcess actors based on the process configuration
    processConfig.foreach { case (id, neighbors) =>
      val isInitiator = id == randomInitiatorId
      val processActor = system.actorOf(Props(new EchoProcess(id, neighbors, isInitiator, processRecord)), id.toString)
      log.info(s"Created EchoProcess actor for Process $id with neighbors: ${neighbors.mkString(", ")}, Actor: $processActor")
      processRecord.map += (id -> processActor)
    }

    // Notify all processes that they are created and can start the algorithm
    processRecord.map.get(randomInitiatorId).foreach(_ ! AllProcessesCreated)

    // Create a Terminator actor to handle the termination of the algorithm
    val terminator = system.actorOf(Props(new Terminator(system)), "terminator")
    processRecord.map += (-1 -> terminator)

    // Wait for the algorithm to terminate or timeout after 30 seconds
    Await.ready(system.whenTerminated, 30.seconds)
    log.info("Algorithm terminated")
  }
}