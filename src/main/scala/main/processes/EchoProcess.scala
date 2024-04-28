package main.processes

import akka.actor.Actor
import akka.event.slf4j.Logger
import main.utility.{EchoWave, EchoTerminate, MessageTypes, AllProcessesCreated, ProcessRecord}

/**
 * Actor representing a process in the Echo algorithm.
 *
 * @param id            The unique identifier of the process.
 * @param neighbors     The list of neighboring process IDs.
 * @param initiator     Flag indicating if this process is the initiator.
 * @param processRecord The ProcessRecord instance for storing process references.
 */
class EchoProcess(val id: String, val neighbors: List[String], val initiator: Boolean, val processRecord: ProcessRecord)
  extends Actor {
  val log = Logger(getClass.getName)
  var received: Int = 0 // Counter for received EchoWave messages
  var parent: Option[String] = None // Optional parent process ID

  /**
   * Receives messages and performs actions based on the message type.
   */
  def receive: Receive = {
    case AllProcessesCreated =>
      if (initiator) {
        log.info(s"$self is the initiator, all processes created, sending Wave to neighbors: $neighbors")
        neighbors.foreach(neighbor => processRecord.map.get(neighbor.toInt).foreach(_ ! EchoWave()))
      }

    case EchoWave() =>
      val senderId = sender().path.name
      received += 1
      log.info(s"$self received Wave from $senderId, received count: $received")

      if (parent.isEmpty && !initiator) {
        parent = Some(senderId)
        log.info(s"$self set $senderId as parent")

        if (neighbors.size > 1) {
          neighbors.foreach { neighbor =>
            if (neighbor != senderId) {
              log.info(s"$self sending Wave to $neighbor")
              processRecord.map.get(neighbor.toInt).foreach(_ ! EchoWave())
            }
          }
        } else {
          log.info(s"$self sending Wave back to $senderId")
          processRecord.map.get(senderId.toInt).foreach(_ ! EchoWave())
        }
      } else if (received == neighbors.size) {
        parent match {
          case Some(parentId) =>
            log.info(s"$self received Wave from all neighbors, sending Wave to parent $parentId")
            processRecord.map.get(parentId.toInt).foreach(_ ! EchoWave())
          case None =>
            log.info(s"$self (initiator) received Wave from all neighbors, deciding")
            processRecord.map.get(-1).foreach(_ ! EchoTerminate())
            context.stop(self)
        }
      }
  }
}
