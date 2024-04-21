package main

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.event.slf4j.Logger

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Messages used in Tarry's algorithm.
 */
case class Initiate()
case class Probe(id: Int)
case object TerminateTarry

/**
 * Terminator actor responsible for terminating the actor system when the algorithm is complete.
 *
 * @param system The actor system to terminate.
 */
class TarryTerminator(system: ActorSystem) extends Actor {
  val log = Logger(getClass.getName)

  override def receive: Receive = {
    case TerminateTarry =>
      log.info("Terminator received TerminateTarry message")
      system.terminate()
  }
}

/**
 * TarryProcess actor representing a node in the topology.
 *
 * @param id        The unique identifier of the process.
 * @param neighbors The list of neighboring process IDs.
 * @param initiator Flag indicating if this process is the initiator.
 */
class TarryProcess(val id: Int, val neighbors: List[Int], val initiator: Boolean) extends Actor {
  val log = Logger(getClass.getName)
  var parent: Int = -1
  var tokensSent: Int = 0
  var tokensReceived: Int = 0
  var sent: mutable.Set[Int] = mutable.Set.empty

  def receive: Receive = {
    case Initiate =>
      log.info(s"$self is the initiator")
      if (neighbors.nonEmpty) {
        val firstNeighbor = neighbors.head
        forward(firstNeighbor)
      }

    case Probe(sid) =>
      log.info(s"$self received token from ${sender()}")
      tokensReceived += 1

      if (parent == -1) {
        parent = sid
      }

      if (initiator && tokensSent == neighbors.size && tokensReceived == neighbors.size) {
        log.info(s"$self received token through all channels, traversal completed")
        TarryProcessRecord.map(-1) ! TerminateTarry
      } else {
        val unsentNeighbors = neighbors.filter(n => !sent.contains(n))
        if (unsentNeighbors.nonEmpty) {
          val nextNeighbor = unsentNeighbors.head
          forward(nextNeighbor)
        } else if (sid != parent) {
          forward(parent)
        }
      }
  }

  /**
   * Forwards the token to the specified neighbor.
   *
   * @param neighborId The ID of the neighbor to forward the token to.
   */
  def forward(neighborId: Int): Unit = {
    TarryProcessRecord.map.get(neighborId) match {
      case Some(actorRef) =>
        log.info(s"$self forwarding token to $actorRef")
        actorRef ! Probe(id)
        tokensSent += 1
        sent += neighborId
      case None =>
        log.error(s"$self Actor reference not found for process $neighborId")
    }
  }
}

/**
 * Object to store the mapping between process IDs and their corresponding ActorRefs.
 */
object TarryProcessRecord {
  val map: mutable.Map[Int, ActorRef] = mutable.Map.empty
}

/**
 * Main object to run Tarry's algorithm.
 */
object TarrysAlgorithm {
  val log = Logger(getClass.getName)

  /**
   * Main method to run Tarry's algorithm.
   */
  def main(): Unit = {
    val system = ActorSystem("TarrysAlgorithm")

    // Read the topology from the input file
    val filename = "/Users/dhruv/Desktop/CS553-DistributedAlgorithms/src/main/scala/main/inputEcho.dot"
    val topologyLines = scala.io.Source.fromFile(filename).getLines().toList

    // Parse the topology and create a map of process IDs and their neighbors
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

    // Create the TarryProcess actors based on the topology
    processConfig.foreach { case (id, neighbors) =>
      val initiator = id == 1
      val process = system.actorOf(Props(new TarryProcess(id, neighbors, initiator)), s"process$id")
      TarryProcessRecord.map += (id -> process)
    }

    // Create the Terminator actor
    val terminator = system.actorOf(Props(new TarryTerminator(system)), "terminator")
    TarryProcessRecord.map += (-1 -> terminator)

    // Initiate the algorithm by sending an Initiate message to the initiator process
    log.info("Initiating the algorithm")
    TarryProcessRecord.map.get(1).foreach(_ ! Initiate)

    // Wait for the algorithm to terminate or timeout after 30 seconds
    Await.ready(system.whenTerminated, 30.seconds)
    log.info("Algorithm terminated")
  }
}