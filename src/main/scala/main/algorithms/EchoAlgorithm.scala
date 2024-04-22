package main.algorithms

import akka.actor.{ActorSystem, Props}
import akka.event.slf4j.Logger
import main.processes.EchoProcess
import main.utility.{MessageTypes, Terminator, ApplicationProperties, TopologyReader}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random

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

    // Load the topology file path from the application configuration
    val filename = ApplicationProperties.echoInputFile
    val processConfig: Map[String, List[String]] = TopologyReader.readTopology(filename)

    // Get all process IDs and convert them to integers
    val allProcessIds = processConfig.keys.map(_.toInt).toList

    // Randomly select the initiator process ID from the list of process IDs
    val randomInitiatorId = Random.shuffle(allProcessIds).head
    log.info(s"Randomly selected initiator ID: $randomInitiatorId")

    processConfig.foreach { case (id, neighbors) =>
      val neighborList = neighbors.mkString(", ") // Convert neighbors list to a string for logging
      val processActor = system.actorOf(Props(new EchoProcess(id, neighbors, id.toInt == randomInitiatorId)), id)
      log.info(s"Created EchoProcess actor for Process $id with neighbors: $neighborList, Actor: $processActor")
    }

    // Create a Terminator actor to handle the termination of the algorithm
    val terminator = system.actorOf(Props(new Terminator(system)), "terminator")

    // Wait for the algorithm to terminate or timeout after 30 seconds
    Await.ready(system.whenTerminated, 30.seconds)
    log.info("Algorithm terminated")
  }
}
