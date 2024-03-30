package main

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

import scala.collection.mutable

case class Initiate()
case class Probe(id: Int, parentId: Int)
case class Reply(id: Int, parent: Int)

class Process(val id: Int, val system: ActorSystem, val neighbors: List[Int]) extends Actor {
  var parent: Int = -1
  var children: List[Int] = List()
  var visited: Boolean = false

  def receive = {
    case Initiate =>
      if (!visited) {
        visited = true
        println(s"Process $id initiated")
        neighbors.foreach(neighbor => {
          println(s"Process $id sending Probe to $neighbor")
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
        } else {
          children.foreach(child => {
            println(s"Process $id sending Probe to child $child")
            ProcessRecord.map.get(child).get ! Probe(id, id)
          })
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
  val processP = system.actorOf(Props(new Process(0, system, List(2))), name = "processP")
  val processQ = system.actorOf(Props(new Process(1, system, List(2))), name = "processQ")
  val processR = system.actorOf(Props(new Process(2, system, List(0, 1, 3))), name = "processR")
  val processS = system.actorOf(Props(new Process(3, system, List(2, 4, 5))), name = "processS")
  val processT = system.actorOf(Props(new Process(4, system, List(3))), name = "processT")
  val processU = system.actorOf(Props(new Process(5, system, List(3))), name = "processU")

  ProcessRecord.map.put(0, processP)
  ProcessRecord.map.put(1, processQ)
  ProcessRecord.map.put(2, processR)
  ProcessRecord.map.put(3, processS)
  ProcessRecord.map.put(4, processT)
  ProcessRecord.map.put(5, processU)

  processP ! Initiate
  processQ ! Initiate
  processT ! Initiate
  processU ! Initiate
}
