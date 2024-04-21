package main.algorithms

import akka.actor.{ActorSystem, Props}
import akka.event.slf4j.Logger
import main.processes.TreeProcess
import main.utility.{Wave, MessageTypes, ProcessRecord, Terminator, ApplicationProperties}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.Source

object TreeAlgorithm extends MessageTypes {
  val log = Logger(getClass.getName)

  /**
   * Main entry point of the tree algorithm.
   * Initializes the actor system, loads the topology from a file, creates the actors,
   * and runs the algorithm.
   */
  def main(): Unit = {
    // Create an actor system for the tree algorithm
    val system = ActorSystem("TreeAlgorithm")
    // Create a ProcessRecord to store the mapping of process IDs to actor references
    val processRecord = new ProcessRecord

    // Load the topology file path from the application configuration
    val filename = ApplicationProperties.treeInputFile
    // Read the topology file and convert it to a list of lines
    val topologyLines = Source.fromFile(filename).getLines().toList

    // Parse the topology lines and create a map of process IDs and their neighbors
    val processConfig: Map[Int, List[Int]] = topologyLines
      .filter(line => line.contains("->"))
      .flatMap { line =>
        val parts = line.split("->")
        if (parts.length == 2) {
          val from = parts(0).trim.replaceAll("\"", "").toInt
          val to = parts(1).split("\\[")(0).trim.replaceAll("\"", "").toInt
          List((from, to), (to, from))
        } else {
          List.empty
        }
      }
      .groupBy(_._1)
      .mapValues(_.map(_._2).toList)
      .toMap

    // Create TreeProcess actors based on the process configuration
    processConfig.foreach { case (id, neighbors) =>
      val process = system.actorOf(Props(new TreeProcess(id, neighbors, processRecord)), s"process$id")
      processRecord.map += (id -> process)
    }

    // Create a Terminator actor to handle the termination of the algorithm
    val terminator = system.actorOf(Props(new Terminator(system)), "terminator")
    processRecord.map.put(-1, terminator)

    log.info("Sending initial Wave messages")
    // Find the leaf nodes (nodes with only one neighbor) and send initial Wave messages to them
    val leafNodes = processConfig.filter { case (_, neighbors) => neighbors.size == 1 }.keys
    leafNodes.foreach(id => processRecord.map(id) ! Wave)

    // Wait for the algorithm to terminate or timeout after 30 seconds
    Await.ready(system.whenTerminated, 30.seconds)
    log.info("Algorithm terminated")
  }
}