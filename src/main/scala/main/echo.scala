import akka.actor.{Actor, ActorRef, ActorSystem, Props}

case class Wave()

class EchoProcess(val id: String, val neighbors: List[String], val initiator: Boolean) extends Actor {
  var received: Int = 0
  var parent: Option[String] = None

  if (initiator) {
    println(s"Process $id is the initiator, sending Wave to neighbors: $neighbors")
    neighbors.foreach(neighbor => context.actorSelection(s"/user/$neighbor") ! Wave())
  }

  def receive: Receive = {
    case Wave() =>
      val sender = getSender(context.sender())
      received += 1
      println(s"Process $id received Wave from $sender, received count: $received")

      if (parent.isEmpty && !initiator) {
        parent = Some(sender)
        println(s"Process $id set $sender as parent")

        if (neighbors.size > 1) {
          neighbors.foreach { neighbor =>
            if (neighbor != sender) {
              println(s"Process $id sending Wave to $neighbor")
              context.actorSelection(s"/user/$neighbor") ! Wave()
            }
          }
        } else {
          println(s"Process $id sending Wave back to $sender")
          context.actorSelection(s"/user/$sender") ! Wave()
        }
      } else if (received == neighbors.size) {
        parent match {
          case Some(parentId) =>
            println(s"Process $id received Wave from all neighbors, sending Wave to parent $parentId")
            context.actorSelection(s"/user/$parentId") ! Wave()
          case None =>
            println(s"Process $id (initiator) received Wave from all neighbors, deciding")
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

  val processP = system.actorOf(Props(new EchoProcess("p", List("s", "q", "t"), initiator = true)), "p")
  val processS = system.actorOf(Props(new EchoProcess("s", List("p", "q", "t"), initiator = false)), "s")
  val processQ = system.actorOf(Props(new EchoProcess("q", List("p", "s", "t", "r"), initiator = false)), "q")
  val processT = system.actorOf(Props(new EchoProcess("t", List("p", "s", "q"), initiator = false)), "t")
  val processR = system.actorOf(Props(new EchoProcess("r", List("q"), initiator = false)), "r")
}