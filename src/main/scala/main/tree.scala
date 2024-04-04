package main

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

import scala.collection.mutable

case class Wave()
case class Info()
case class Decide()

class Process(val id: Int, val neighbors: List[Int]) extends Actor {
  var received: Map[Int, Boolean] = neighbors.map(_ -> false).toMap
  var parent: Option[Int] = None

  println(s"Process $id initialized with neighbors: $neighbors")

  def receive: Receive = {
    case Wave =>
      val sender = getSender(context.sender())
      received += (sender -> true)
      println(s"Process $id received Wave from Process $sender")
      sendWave()

    case Info =>
      val sender = getSender(context.sender())
      println(s"Process $id received Info from Process $sender")
      if (parent.contains(sender)) {
        neighbors.foreach { neighbor =>
          if (neighbor != sender) {
            println(s"Process $id sending Info to Process $neighbor")
            ProcessRecord.map(neighbor) ! Info
          }
        }
      }

    case Decide =>
      println(s"Process $id decided")
  }

  def sendWave(): Unit = {
    val unreceivedNeighbors = neighbors.filterNot(received)
    if (unreceivedNeighbors.size == 1) {
      val neighbor = unreceivedNeighbors.head
      println(s"Process $id sending Wave to Process $neighbor")
      ProcessRecord.map(neighbor) ! Wave
      parent = Some(neighbor)
      println(s"Process $id set Process $neighbor as parent")
    } else if (unreceivedNeighbors.isEmpty) {
      println(s"Process $id sending Decide to itself")
      self ! Decide
      neighbors.foreach { neighbor =>
        if (neighbor != parent.getOrElse(-1)) {
          println(s"Process $id sending Info to Process $neighbor")
          ProcessRecord.map(neighbor) ! Info
        }
      }
    }
  }

  def getSender(actorRef: ActorRef): Int = {
    ProcessRecord.map.find(_._2 == actorRef).map(_._1).getOrElse(-1)
  }
}

object ProcessRecord {
  val map: mutable.Map[Int, ActorRef] = mutable.Map.empty
}

object TreeAlgorithm extends App {
  val system = ActorSystem("TreeAlgorithmSystem")

  val processP = system.actorOf(Props(new Process(0, List(2))), "processP")
  val processQ = system.actorOf(Props(new Process(1, List(2))), "processQ")
  val processR = system.actorOf(Props(new Process(2, List(0, 1, 3))), "processR")
  val processS = system.actorOf(Props(new Process(3, List(2, 4, 5))), "processS")
  val processT = system.actorOf(Props(new Process(4, List(3))), "processT")
  val processU = system.actorOf(Props(new Process(5, List(3))), "processU")

  ProcessRecord.map ++= Map(
    0 -> processP,
    1 -> processQ,
    2 -> processR,
    3 -> processS,
    4 -> processT,
    5 -> processU
  )

  println("Sending initial Wave messages")
  processP ! Wave
  processQ ! Wave
  processT ! Wave
  processU ! Wave
}
