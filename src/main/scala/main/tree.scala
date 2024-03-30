package main

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

import scala.collection.mutable

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
        neighbors.foreach(neighbor => {
          println(s"Process $id initiated and sending Probe to $neighbor")
          ProcessRecord.map.get(neighbor).get ! Probe(id, -1)
        })
      }

    case Probe(sid, parentId) =>
      if (!visited) {
        visited = true
        parent = parentId
        children = neighbors.filter(_ != parentId)
        println(s"Process $id received Probe from $sid, setting parent to $parentId")
        if (children.isEmpty) {
          println(s"Process $id has no children, sending Reply to $parent")
          ProcessRecord.map.get(parent).get ! Reply(id, parent)
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
  val processP = system.actorOf(Props(new Process(0, system, List(3))), name = "processP")
  val processQ = system.actorOf(Props(new Process(1, system, List(3))), name = "processQ")
  val processR = system.actorOf(Props(new Process(2, system, List(4, 5))), name = "processR")
  val processS = system.actorOf(Props(new Process(3, system, List(6, 7))), name = "processS")
  val processT = system.actorOf(Props(new Process(4, system, List())), name = "processT")
  val processU = system.actorOf(Props(new Process(5, system, List())), name = "processU")

  ProcessRecord.map.put(0, processP)
  ProcessRecord.map.put(1, processQ)
  ProcessRecord.map.put(2, processR)
  ProcessRecord.map.put(3, processS)
  ProcessRecord.map.put(4, processT)
  ProcessRecord.map.put(5, processU)

  processP ! Initiate
  processQ ! Initiate
}