package main.utility

import akka.actor.{Actor, ActorSystem}

case class Terminate()

class Terminator(val system: ActorSystem) extends Actor {

  override def receive: Receive = {
    case Terminate =>
      println("Terminating system")
      system.terminate()
  }

}