package main.utility

import akka.actor.{Actor, ActorSystem}


/**
 *
 * @param system
 *
 * Utility class for terminating the Actor system.
 * Based on the message type. initiates system or process termination
 */
class Terminator(val system: ActorSystem) extends Actor {

  override def receive: Receive = {
    case TerminateSystem =>
      println("Terminating system")
      system.terminate()

    case TerminateProcess(processRecord: ProcessRecord) =>{
        processRecord.map.foreach(process=>{
          if(process._1 != -1){
            system.stop(process._2)

          }
      })
    }
  }

}