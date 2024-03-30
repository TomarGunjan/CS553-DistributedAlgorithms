import akka.actor.{Actor, ActorRef, ActorSystem, Props}

import java.util
import scala.collection.mutable




case class Initiate()
case class Probe(id: Int)
case class Report(id: Int)

class Process(val id: Int, val system: ActorSystem, val neighbors: List[Int]) extends Actor {
  var parent: ActorRef = _
  var children: List[ActorRef] = List()
  var visited: Boolean = false

  def receive = {
    case Initiate =>
      if (!visited) {
        visited = true
        parent = sender
        for (n <- neighbors){
          var p: ActorRef = ProcessRecord.map.get(n).get;
          if (p!=None){
             p ! Probe(id)
          }
        }
      }

    case Probe(id) =>
      if (!visited) {
        visited = true
        parent = sender
        //children = neighbors.map(id => system.actorOf(Props(new Process(id, system, neighbors.filter(_ != id))), name = s"process$id"))
        //children = ProcessRecord.map.get(id)
        neighbors.foreach(pr=> ProcessRecord.map.get(pr).get ! Probe(id))
        //children.foreach(_ ! Probe(id))
      } else {
        sender ! Report(id)
      }

    case Report(id) =>
      if (sender != parent) {
        children = children.filter(_ != sender)
      }
      if (children.isEmpty) {
        parent ! Report(id)
      }
  }
}

object ProcessRecord{
  val map = mutable.Map.empty[Int, ActorRef]
}

object TarrysAlgorithm extends App {
  val system = ActorSystem("TarrysAlgorithmSystem")
  val process1 = system.actorOf(Props(new Process(1, system, List(2, 4, 5))), name = "process1")
  val process2 = system.actorOf(Props(new Process(2, system, List(1, 3, 5))), name = "process2")
  val process3 = system.actorOf(Props(new Process(3, system, List(2))), name = "process3")
  val process4 = system.actorOf(Props(new Process(4, system, List(1, 5))), name = "process4")
  val process5 = system.actorOf(Props(new Process(5, system, List(1, 2, 4))), name = "process5")



  ProcessRecord.map.put(1, process1)
  ProcessRecord.map.put(2,process2)
  ProcessRecord.map.put(3,process3)
  ProcessRecord.map.put(4,process4)
  ProcessRecord.map.put(5,process5)

  process1 ! Initiate
}
