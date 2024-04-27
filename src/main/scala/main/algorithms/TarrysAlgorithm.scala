package main.algorithms

import akka.actor.{ActorSystem, Props}
import akka.event.slf4j.Logger
import main.processes.TarryProcess
import main.utility.{InitiateTarry, MessageTypes, ProcessRecord, Terminator, TopologyReader, ApplicationProperties}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random

/**
 * Object representing Tarry's algorithm.
 * Extends the MessageTypes trait to have access to the message types used in the algorithm.
 */
object TarrysAlgorithm extends MessageTypes {
  val log = Logger(getClass.getName)

  /**
   * Main entry point of Tarry's algorithm.
   * Initializes the actor system, loads the topology from a file, creates the actors,
   * and runs the algorithm.
   */
  def main(): Unit = {
    // Create an actor system for Tarry's algorithm
    val system = ActorSystem("TarrysAlgorithm")
    // Create a ProcessRecord to store the mapping of process IDs to actor references
    val processRecord = new ProcessRecord

    // Load the topology file path from the application configuration
    val filename = ApplicationProperties.tarryInputFile
    val processConfig: Map[String, List[String]] = TopologyReader.readTopology(filename)

    // Get all process IDs and convert them to integers
    val allProcessIds = processConfig.keys.map(_.toInt).toList
    // Randomly select the initiator process ID from the list of process IDs
    val randomInitiatorId = Random.shuffle(allProcessIds).head
    log.info(s"Randomly selected initiator ID: $randomInitiatorId")

    // Create TarryProcess actors based on the process configuration
    processConfig.foreach { case (id, neighbors) =>
      val neighborList = neighbors.mkString(", ") // Convert neighbors list to a string for logging
      val initiator = id.toInt == randomInitiatorId
      val processActor = system.actorOf(Props(new TarryProcess(id.toInt, neighbors.map(_.toInt), initiator, processRecord)), s"process$id")
      log.info(s"Created TarryProcess actor for Process $id with neighbors: $neighborList, Actor: $processActor")
      processRecord.map += (id.toInt -> processActor)
    }

    // Create a Terminator actor to handle the termination of the algorithm
    val terminator = system.actorOf(Props(new Terminator(system)), "terminator")
    processRecord.map += (-1 -> terminator)

    log.info("Initiating the algorithm")
    // Send an InitiateTarry message to the randomly selected initiator process
    processRecord.map.get(randomInitiatorId).foreach(_ ! InitiateTarry)

    // Wait for the algorithm to terminate or timeout after 30 seconds
    Await.ready(system.whenTerminated, 30.seconds)
    log.info("Algorithm terminated")
  }
}