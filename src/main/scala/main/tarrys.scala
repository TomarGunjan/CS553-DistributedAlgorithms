package main

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

import scala.collection.mutable
import java.time.LocalTime
import scala.language.postfixOps

case class Initiate()
case class Probe(id: Int)
case class Report(id: Int)

class TarryProcess(val id: Int, val neighbors: List[Int], val initiator: Boolean) extends Actor {
  var parent: Int = -1
  var children: mutable.Set[Int] = mutable.Set.empty
  var visited: mutable.Set[Int] = mutable.Set.empty

  def receive = {
    case Initiate =>
      println(s"${LocalTime.now()} - $self is the initiator")
      if (neighbors.nonEmpty) {
        val firstNeighbor = neighbors.head
        forward(firstNeighbor)
      }

    case Probe(sid) =>
      println(s"${LocalTime.now()} - $self received token from ${sender()}")
      if (parent == -1) {
        parent = sid
      }
      visited += sid

      if (children.size == neighbors.size - 1) {
        if (!initiator) {
          forward(parent)
        }
      } else {
        val unvisitedNeighbors = neighbors.toSet -- visited -- children
        if (unvisitedNeighbors.nonEmpty) {
          val nextNeighbor = unvisitedNeighbors.head
          forward(nextNeighbor)
        } else if (sid != parent) {
          forward(parent)
        }
      }

      if (children.size == neighbors.size && initiator) {
        println(s"${LocalTime.now()} - $self received token, traversal completed")
        context.system.terminate()
      }
  }

  def forward(neighborId: Int): Unit = {
    if (!children.contains(neighborId)) {
      TarryProcessRecord.map.get(neighborId) match {
        case Some(actorRef) =>
          println(s"${LocalTime.now()} - $self forwarding token to $actorRef")
          actorRef ! Probe(id)
          children += neighborId
        case None =>
          println(s"${LocalTime.now()} - $self Actor reference not found for process $neighborId")
      }
    }
  }
}

object TarryProcessRecord {
  val map: mutable.Map[Int, ActorRef] = mutable.Map.empty
}

object TarrysAlgorithm extends App {
  val system = ActorSystem("TarrysAlgorithm")

  val filename = "input.txt"
  val topologyLines = scala.io.Source.fromFile(filename).getLines().toList

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

  processConfig.foreach { case (id, neighbors) =>
    val initiator = id == 1
    val process = system.actorOf(Props(new TarryProcess(id, neighbors, initiator)), s"process$id")
    TarryProcessRecord.map += (id -> process)
  }

  println(s"${LocalTime.now()} - Initiating the algorithm")
  TarryProcessRecord.map.get(1).foreach(_ ! Initiate)
}
