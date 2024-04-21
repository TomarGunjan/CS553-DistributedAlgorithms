package main.processes

import akka.actor.{Actor, ActorRef}
import akka.event.slf4j.Logger
import main.utility.{EchoWave,EchoTerminate, MessageTypes}

class EchoProcess(val id: String, val neighbors: List[String], val initiator: Boolean)
  extends Actor with MessageTypes {
  val log = Logger(getClass.getName)
  var received: Int = 0
  var parent: Option[String] = None

  if (initiator) {
    log.info(s"$self is the initiator, sending Wave to neighbors: $neighbors")
    neighbors.foreach(neighbor => context.actorSelection(s"/user/$neighbor") ! EchoWave())
  }

  def receive: Receive = {
    case EchoWave() =>
      val senderId = getSender(context.sender())
      received += 1
      log.info(s"$self received Wave from $senderId, received count: $received")

      if (parent.isEmpty && !initiator) {
        parent = Some(senderId)
        log.info(s"$self set $senderId as parent")

        if (neighbors.size > 1) {
          neighbors.foreach { neighbor =>
            if (neighbor != senderId) {
              log.info(s"$self sending Wave to $neighbor")
              context.actorSelection(s"/user/$neighbor") ! EchoWave()
            }
          }
        } else {
          log.info(s"$self sending Wave back to $senderId")
          context.actorSelection(s"/user/$senderId") ! EchoWave()
        }
      } else if (received == neighbors.size) {
        parent match {
          case Some(parentId) =>
            log.info(s"$self received Wave from all neighbors, sending Wave to parent $parentId")
            context.actorSelection(s"/user/$parentId") ! EchoWave()
          case None =>
            log.info(s"$self (initiator) received Wave from all neighbors, deciding")
            context.actorSelection("/user/terminator") ! EchoTerminate()
            context.stop(self)
        }
      }
  }

  def getSender(actorRef: ActorRef): String = {
    actorRef.path.name
  }
}