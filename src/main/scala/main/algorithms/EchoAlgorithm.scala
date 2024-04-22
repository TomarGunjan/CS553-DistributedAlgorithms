package main.algorithms

import akka.actor.{ActorSystem, Props}
import akka.event.slf4j.Logger
import main.processes.EchoProcess
import main.utility.{MessageTypes, Terminator, ApplicationProperties, TopologyReader}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.Source

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

    processConfig.foreach { case (id, neighbors) =>
      val initiator = id == "1" // Assuming process with ID "1" is the initiator
      val process = system.actorOf(Props(new EchoProcess(id, neighbors, initiator)), id)
    }

    // Create a Terminator actor to handle the termination of the algorithm
    val terminator = system.actorOf(Props(new Terminator(system)), "terminator")

    // Wait for the algorithm to terminate or timeout after 30 seconds
    Await.ready(system.whenTerminated, 30.seconds)
    log.info("Algorithm terminated")
  }
}