package main.algorithms

import akka.actor.{ActorSystem, Props}
import akka.event.slf4j.Logger
import main.processes.TarryProcess
import main.utility.{InitiateTarry, MessageTypes, ProcessRecord, Terminator, TopologyReader, ApplicationProperties}

import scala.concurrent.Await
import scala.concurrent.duration._

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

    processConfig.foreach { case (id, neighbors) =>
      val initiator = id == "1" // Assuming process with ID "1" is the initiator
      val process = system.actorOf(Props(new TarryProcess(id.toInt, neighbors.map(_.toInt), initiator, processRecord)), s"process$id")
      processRecord.map += (id.toInt -> process)
    }


    // Create a Terminator actor to handle the termination of the algorithm
    val terminator = system.actorOf(Props(new Terminator(system)), "terminator")
    processRecord.map += (-1 -> terminator)

    log.info("Initiating the algorithm")
    // Send an InitiateTarry message to the initiator process (ID 1)
    processRecord.map.get(1).foreach(_ ! InitiateTarry)

    // Wait for the algorithm to terminate or timeout after 30 seconds
    Await.ready(system.whenTerminated, 30.seconds)
    log.info("Algorithm terminated")
  }
}