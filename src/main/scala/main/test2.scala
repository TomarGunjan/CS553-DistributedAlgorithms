package main

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

import java.util
import scala.collection.mutable
import scala.language.postfixOps
import scala.util.Random


case class Initiate()
case class Probe(id: Int)
case class Report(id: Int)

class Process(val id: Int, val system: ActorSystem, val neighbors: List[Int], val initiator:Boolean) extends Actor {
 var parent: Integer = -1
 var children: List[Int] = List()
 var visited: Boolean = false

 override def preStart(): Unit = {
   super.preStart()
   //neighbors.foreach(pid => context.watch(ProcessRecord.map.get(pid).get))
 }


 def receive = {
   case Initiate =>
     println("process "+id+" started")
     parent = -2
     val first = neighbors.head
     children = first:: children
     ProcessRecord.map.get(first).get ! Probe(id)


   case Probe(sid) =>
     println("Probe received at "+id+" from "+sid)
     if( parent== -1) {
       parent = sid
     }
     if (children.size == neighbors.size-1) {
       if(!initiator){
       ProcessRecord.map.get(parent).get ! Probe(id)}
     } else {
       val rdmnums = Random.shuffle(neighbors)
       val idx = findNextNeighbor(rdmnums)
       if (idx != -1) {
         ProcessRecord.map.get(idx).get ! Probe(id)
         children = idx :: children
       }
//        ProcessRecord.map.get(idx).get ! Probe(id)
     }
     if(children.size == neighbors.size && initiator){
       println("completed")
     }


 }

 private def findNextNeighbor(neighbors: List[Int]): Int = {
   neighbors.find(num => !children.contains(num) && num != parent) match {
     case Some(idx) => idx
     case None => -1
   }
 }
}

object ProcessRecord{
 val map = mutable.Map.empty[Int, ActorRef]
}

object TarrysAlgorithm2 extends App {
 val system = ActorSystem("TarrysAlgorithmSystem")
//  val process1 = system.actorOf(Props(new Process(1, system, List(2, 4, 5), true)), name = "process1")
//  val process2 = system.actorOf(Props(new Process(2, system, List(1, 3, 5),false)), name = "process2")
//  val process3 = system.actorOf(Props(new Process(3, system, List(2),false)), name = "process3")
//  val process4 = system.actorOf(Props(new Process(4, system, List(1, 5),false)), name = "process4")
//  val process5 = system.actorOf(Props(new Process(5, system, List(1, 2, 4),false)), name = "process5")
val process0 = system.actorOf(Props(new Process(0, system, List(1), true)), name = "process0")
   val process1 = system.actorOf(Props(new Process(1, system, List(3,4,2), false)), name = "process1")
   val process2 = system.actorOf(Props(new Process(2, system, List(5,6,1),false)), name = "process2")
   val process3 = system.actorOf(Props(new Process(3, system, List(1,5),false)), name = "process3")
   val process4 = system.actorOf(Props(new Process(4, system, List(1,6),false)), name = "process4")
   val process5 = system.actorOf(Props(new Process(5, system, List(2,3),false)), name = "process5")
 val process6 = system.actorOf(Props(new Process(6, system, List(2,4),false)), name = "process6")



 ProcessRecord.map.put(0, process0)
 ProcessRecord.map.put(1, process1)
 ProcessRecord.map.put(2,process2)
 ProcessRecord.map.put(3,process3)
 ProcessRecord.map.put(4,process4)
 ProcessRecord.map.put(5,process5)
 ProcessRecord.map.put(6,process6)

 process0 ! Initiate
}
