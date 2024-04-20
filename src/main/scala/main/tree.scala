package main

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import scala.collection.mutable
import scala.io.Source
import java.time.LocalTime

case class Wave()
case class Info()
case class Decide()

class TreeProcess(val id: Int, val neighbors: List[Int]) extends Actor {
  var received: Map[Int, Boolean] = neighbors.map(_ -> false).toMap
  var parent: Option[Int] = None

  println(s"${LocalTime.now()} - [$self] initialized with neighbors: $neighbors")

  def receive: Receive = {
    case Wave =>
      val senderId = getSender(context.sender())
      received += (senderId -> true)
      println(s"${LocalTime.now()} - [$self] received Wave from [${context.sender()}]")
      sendWave()

    case Info =>
      val senderId = getSender(context.sender())
      println(s"${LocalTime.now()} - [$self] received Info from [${context.sender()}]")
      if (parent.contains(senderId)) {
        neighbors.foreach { neighbor =>
          if (neighbor != senderId) {
            println(s"${LocalTime.now()} - [$self] sending Info to [${TreeProcessRecord.map(neighbor)}]")
            TreeProcessRecord.map(neighbor) ! Info
          }
        }
      }

    case Decide =>
      println(s"${LocalTime.now()} - [$self] decided")
  }

  def sendWave(): Unit = {
    val unreceivedNeighbors = neighbors.filterNot(received)
    if (unreceivedNeighbors.size == 1) {
      val neighbor = unreceivedNeighbors.head
      println(s"${LocalTime.now()} - [$self] sending Wave to [${TreeProcessRecord.map(neighbor)}]")
      TreeProcessRecord.map(neighbor) ! Wave
      parent = Some(neighbor)
      println(s"${LocalTime.now()} - [$self] set [${TreeProcessRecord.map(neighbor)}] as parent")
    } else if (unreceivedNeighbors.isEmpty) {
      println(s"${LocalTime.now()} - [$self] sending Decide to itself")
      self ! Decide
      neighbors.foreach { neighbor =>
        if (neighbor != parent.getOrElse(-1)) {
          println(s"${LocalTime.now()} - [$self] sending Info to [${TreeProcessRecord.map(neighbor)}]")
          TreeProcessRecord.map(neighbor) ! Info
        }
      }
    }
  }

  def getSender(actorRef: ActorRef): Int = {
    TreeProcessRecord.map.find(_._2 == actorRef).map(_._1).getOrElse(-1)
  }
}

object TreeProcessRecord {
  val map: mutable.Map[Int, ActorRef] = mutable.Map.empty
}

object TreeAlgorithm extends App {
  val system = ActorSystem("TreeAlgorithm")

  // Read the tree topology from the text file
  val filename = "NetGraph_30-03-24-18-54-55.ngs.dot"
  val topologyLines = Source.fromFile(filename).getLines().toList

  // Parse the topology and create a map of process IDs and their neighbors
  val processConfig: Map[Int, List[Int]] = topologyLines
    .filter(line => line.contains("->"))
    .flatMap { line =>
      val parts = line.split("->")
      if (parts.length == 2) {
        val from = parts(0).trim.replaceAll("\"", "").toInt
        val to = parts(1).split("\\[")(0).trim.replaceAll("\"", "").toInt // Remove weight attribute
        List((from, to), (to, from))
      } else {
        List.empty
      }
    }
    .groupBy(_._1)
    .mapValues(_.map(_._2).toList)
    .toMap

  // Create the actors dynamically based on the topology
  processConfig.foreach { case (id, neighbors) =>
    val process = system.actorOf(Props(new TreeProcess(id, neighbors)), s"process$id")
    TreeProcessRecord.map += (id -> process)
  }

  println(s"${LocalTime.now()} - Sending initial Wave messages")
  val leafNodes = processConfig.filter { case (_, neighbors) => neighbors.size == 1 }.keys
  leafNodes.foreach(id => TreeProcessRecord.map(id) ! Wave)
}
