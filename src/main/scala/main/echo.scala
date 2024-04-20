import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import scala.io.Source
import java.time.LocalTime

case class Wave()

class EchoProcess(val id: String, val neighbors: List[String], val initiator: Boolean) extends Actor {
  var received: Int = 0
  var parent: Option[String] = None

  if (initiator) {
    println(s"${LocalTime.now()} - $self is the initiator, sending Wave to neighbors: $neighbors")
    neighbors.foreach(neighbor => context.actorSelection(s"/user/$neighbor") ! Wave())
  }

  def receive: Receive = {
    case Wave() =>
      val senderId = getSender(context.sender())
      received += 1
      println(s"${LocalTime.now()} - $self received Wave from $senderId, received count: $received")

      if (parent.isEmpty && !initiator) {
        parent = Some(senderId)
        println(s"${LocalTime.now()} - $self set $senderId as parent")

        if (neighbors.size > 1) {
          neighbors.foreach { neighbor =>
            if (neighbor != senderId) {
              println(s"${LocalTime.now()} - $self sending Wave to $neighbor")
              context.actorSelection(s"/user/$neighbor") ! Wave()
            }
          }
        } else {
          println(s"${LocalTime.now()} - $self sending Wave back to $senderId")
          context.actorSelection(s"/user/$senderId") ! Wave()
        }
      } else if (received == neighbors.size) {
        parent match {
          case Some(parentId) =>
            println(s"${LocalTime.now()} - $self received Wave from all neighbors, sending Wave to parent $parentId")
            context.actorSelection(s"/user/$parentId") ! Wave()
          case None =>
            println(s"${LocalTime.now()} - $self (initiator) received Wave from all neighbors, deciding")
            context.stop(self)
        }
      }
  }

  def getSender(actorRef: ActorRef): String = {
    actorRef.path.name
  }
}

object EchoAlgorithm extends App {
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
    val initiator = id == "1" // Assuming "p" is the initiator
    val process = system.actorOf(Props(new EchoProcess(id, neighbors, initiator)), id)
  }
}
