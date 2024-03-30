package main

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

import scala.collection.mutable
import scala.util.Random


case class Initiate()
case class Probe(id: Int, parent: Int)
case class Reply(id: Int, parent: Int)

class Process(val id: Int, val system: ActorSystem, val neighbors: List[Int]) extends Actor {
  var parent: Int = -1
  var children: List[Int] = List()
  var visited: Boolean = false

  def receive = {
    case Initiate =>
      if (!visited) {
        visited = true
        val randomNeighbor = Random.shuffle(neighbors).head
        println(s"Process $id initiated and sending Probe to $randomNeighbor")
        ProcessRecord.map.get(randomNeighbor).get ! Probe(id, -1)
      }

    case Probe(sid, parentId) =>
      if (!visited) {
        visited = true
        parent = parentId
        children = neighbors.filter(_ != parentId)
        println(s"Process $id received Probe from $sid, setting parent to $parentId")
        children.foreach(child => {
          println(s"Process $id sending Probe to $child")
          ProcessRecord.map.get(child).get ! Probe(id, id)
        })
        if (children.isEmpty) {
          println(s"Process $id has no children, sending Reply to $parent")
          sender ! Reply(id, parent)
        }
      } else {
        println(s"Process $id received duplicate Probe from $sid, sending Reply to $sid")
        sender ! Reply(id, parent)
      }

    case Reply(cid, cparent) =>
      if (cparent == id) {
        children = children.filter(_ != cid)
        println(s"Process $id received Reply from $cid")
        if (children.isEmpty && parent != -1) {
          println(s"Process $id has no more children, sending Reply to $parent")
          ProcessRecord.map.get(parent).get ! Reply(id, parent)
        }
      }
  }
}

object ProcessRecord {
  val map = mutable.Map.empty[Int, ActorRef]
}

object TreeAlgorithm extends App {
  val system = ActorSystem("TreeAlgorithmSystem")
  val process0 = system.actorOf(Props(new Process(0, system, List(1))), name = "process0")
  val process1 = system.actorOf(Props(new Process(1, system, List(0, 2, 3))), name = "process1")
  val process2 = system.actorOf(Props(new Process(2, system, List(1, 4))), name = "process2")
  val process3 = system.actorOf(Props(new Process(3, system, List(1, 5))), name = "process3")
  val process4 = system.actorOf(Props(new Process(4, system, List(2))), name = "process4")
  val process5 = system.actorOf(Props(new Process(5, system, List(3))), name = "process5")

  ProcessRecord.map.put(0, process0)
  ProcessRecord.map.put(1, process1)
  ProcessRecord.map.put(2, process2)
  ProcessRecord.map.put(3, process3)
  ProcessRecord.map.put(4, process4)
  ProcessRecord.map.put(5, process5)

  process0 ! Initiate
}