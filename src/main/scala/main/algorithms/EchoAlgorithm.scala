package main.algorithms

import akka.actor.{ActorSystem, Props}
import akka.event.slf4j.Logger
import main.processes.EchoProcess
import main.utility.{MessageTypes, Terminator, ApplicationProperties}

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
    // Read the topology file and convert it to a list of lines
    val topologyLines = Source.fromFile(filename).getLines().toList

    // Parse the topology lines and create a map of process IDs and their neighbors
    val processConfig: Map[String, List[String]] = topologyLines
      .filter(line => line.contains("->"))
      .flatMap { line =>
        val parts = line.split("->")
        if (parts.length == 2) {
          val from = parts(0).trim.replaceAll("\"", "")
          val to = parts(1).split("\\[")(0).trim.replaceAll("\"", "")
          List((from, to), (to, from))
        } else {
          List.empty
        }
      }
      .groupBy(_._1)
      .mapValues(_.map(_._2).toList)
      .toMap

    // Create EchoProcess actors based on the process configuration
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