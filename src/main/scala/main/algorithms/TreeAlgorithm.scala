package main.algorithms

import akka.actor.{ActorSystem, Props}
import akka.event.slf4j.Logger
import main.processes.TreeProcess
import main.utility.{ApplicationProperties, MessageTypes, ProcessRecord, Terminator, TopologyReader, Wave}

import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Object representing the Tree algorithm.
 * Extends the MessageTypes trait to have access to the message types used in the algorithm.
 */
object TreeAlgorithm extends MessageTypes {
  val log = Logger(getClass.getName)

  /**
   * Main method to run the Tree algorithm.
   */
  def main(): Unit = {
    val system = ActorSystem("TreeAlgorithm")
    val processRecord = new ProcessRecord

    val filename = ApplicationProperties.treeInputFile
    val processConfig: Map[String, List[String]] = TopologyReader.readTopology(filename)

    // Check for the presence of a cycle in the topology
    if (TopologyReader.hasCycle(processConfig)) {
      log.error("The input topology contains a cycle. Terminating the algorithm.")
      system.terminate()
      return
    }

    // Create TreeProcess actors based on the topology
    processConfig.foreach { case (id, neighbors) =>
      val process = system.actorOf(Props(new TreeProcess(id.toInt, neighbors.map(_.toInt), processRecord)), s"process$id")
      processRecord.map += (id.toInt -> process)
    }

    // Create a Terminator actor to handle the termination of the algorithm
    val terminator = system.actorOf(Props(new Terminator(system)), "terminator")
    processRecord.map.put(-1, terminator)

    log.info("Sending initial Wave messages")
    val leafNodes = processConfig.filter { case (_, neighbors) => neighbors.size == 1 }.keys
    leafNodes.foreach(id => processRecord.map(id.toInt) ! Wave)

    Await.ready(system.whenTerminated, 30.seconds)
    log.info("Algorithm terminated")
  }
}
