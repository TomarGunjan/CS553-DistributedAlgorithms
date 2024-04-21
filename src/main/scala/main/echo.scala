package main

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.event.slf4j.Logger
import scala.io.Source
import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Message used in the Echo algorithm.
 */
case class EchoWave()

/**
 * Terminator actor responsible for terminating the actor system when the algorithm is complete.
 *
 * @param system The actor system to terminate.
 */
class EchoTerminator(system: ActorSystem) extends Actor {
  val log = Logger(getClass.getName)

  override def receive: Receive = {
    case "terminate" =>
      log.info("Terminator received terminate message")
      system.terminate()
  }
}

/**
 * EchoProcess actor representing a node in the topology.
 *
 * @param id        The unique identifier of the process.
 * @param neighbors The list of neighboring process IDs.
 * @param initiator Flag indicating if this process is the initiator.
 */
class EchoProcess(val id: String, val neighbors: List[String], val initiator: Boolean) extends Actor {
  val log = Logger(getClass.getName)
  var received: Int = 0
  var parent: Option[String] = None

  if (initiator) {
    log.info(s"$self is the initiator, sending Wave to neighbors: $neighbors")
    neighbors.foreach(neighbor => context.actorSelection(s"/user/$neighbor") ! EchoWave())
  }

  def receive: Receive = {
    case EchoWave() =>
      val senderId = getSender(context.sender())
      received += 1
      log.info(s"$self received Wave from $senderId, received count: $received")

      if (parent.isEmpty && !initiator) {
        parent = Some(senderId)
        log.info(s"$self set $senderId as parent")

        if (neighbors.size > 1) {
          neighbors.foreach { neighbor =>
            if (neighbor != senderId) {
              log.info(s"$self sending Wave to $neighbor")
              context.actorSelection(s"/user/$neighbor") ! EchoWave()
            }
          }
        } else {
          log.info(s"$self sending Wave back to $senderId")
          context.actorSelection(s"/user/$senderId") ! EchoWave()
        }
      } else if (received == neighbors.size) {
        parent match {
          case Some(parentId) =>
            log.info(s"$self received Wave from all neighbors, sending Wave to parent $parentId")
            context.actorSelection(s"/user/$parentId") ! EchoWave()
          case None =>
            log.info(s"$self (initiator) received Wave from all neighbors, deciding")
            context.actorSelection("/user/terminator") ! "terminate"
            context.stop(self)
        }
      }
  }

  /**
   * Utility method to get the sender ID from the ActorRef.
   *
   * @param actorRef The ActorRef of the sender.
   * @return The sender ID.
   */
  def getSender(actorRef: ActorRef): String = {
    actorRef.path.name
  }
}

/**
 * Main object to run the Echo algorithm.
 */
object EchoAlgorithm {
  val log = Logger(getClass.getName)

  /**
   * Main method to run the Echo algorithm.
   */
  def main(): Unit = {
    val system = ActorSystem("EchoAlgorithmSystem")

    // Read the input file to get process IDs and neighbors
    val filename = "/Users/dhruv/Desktop/553/CS553-DistributedAlgorithms/src/main/scala/main/inputEcho.dot"
    val topologyLines = Source.fromFile(filename).getLines().toList

    // Parse the topology and create actors dynamically
    val processConfig: Map[String, List[String]] = topologyLines
      .filter(line => line.contains("->"))
      .flatMap { line =>
        val parts = line.split("->")
        if (parts.length == 2) {
          val from = parts(0).trim.replaceAll("\"", "")
          val to = parts(1).split("\\[")(0).trim.replaceAll("\"", "") // Remove weight attribute
          List((from, to), (to, from))
        } else {
          List.empty
        }
      }
      .groupBy(_._1)
      .mapValues(_.map(_._2).toList)
      .toMap

    // Create actors dynamically based on the topology
    processConfig.foreach { case (id, neighbors) =>
      val initiator = id == "1" // Assuming "1" is the initiator
      val process = system.actorOf(Props(new EchoProcess(id, neighbors, initiator)), id)
    }

    // Create the terminator actor
    val terminator = system.actorOf(Props(new EchoTerminator(system)), "terminator")

    // Wait for the algorithm to terminate or timeout after 30 seconds
    Await.ready(system.whenTerminated, 30.seconds)
    log.info("Algorithm terminated")
  }
}