package main.processes

import akka.actor.{Actor, ActorSystem}
import akka.event.slf4j.Logger
import main.utility.{Ack, Done, Grant, Initiate, Notify, ProcessRecord, TerminateProcess, TerminateSystem}

import scala.collection.mutable


/**
 *
 * @param id
 * @param system
 * @param out
 * @param in
 * @param initiator
 * @param processRecord
 *
 * Process class for BrachaToueg Algorithm
 *
 *
 */
class BrachaTouegProcess(val id: Int, val system: ActorSystem, val out: List[Integer], val in : List[Integer], val initiator:Boolean, val processRecord: ProcessRecord) extends Actor {
  var notifyFlag = false
  var free=false
  var requests = out.size
  var done=out.size
  var ack = in.size
  var neighborNotify = mutable.Stack[Int]()
  var neighborGrant = mutable.Stack[Int]()
  val logger = Logger(getClass.getName)


  override def preStart(): Unit = {
    super.preStart()
  }


  def receive = {
    case Initiate =>
      logger.debug("Initiated by process "+id)
      notifyProc()


    case Notify(nid) =>
      logger.debug("Notify received at Process "+id+" from Process "+nid)
      if (!notifyFlag) {
        neighborNotify.push(nid)
        notifyProc()
      } else{
        processRecord.map.get(nid).get ! Done
      }


    case Grant(nid) =>
      logger.debug("grant received at "+id+" from "+nid)
      if (requests>0) {
        requests-=1
        if (requests==0){
          println("total request 0 at "+id+" executing grant procedures")
          neighborGrant.push(nid)
          grant()
        }else {
          processRecord.map.get(nid).get ! Ack
        }
        sendAck()
      }



    case Done =>
      done-=1
      logger.debug("received done at process "+id+". Total ack left "+done)
      if (done==0){
        sendDone()
      }


    case Ack =>
      ack-=1
      logger.debug("received acknowledgement at process "+id+". Total ack left "+ack)
      sendAck()
      sendDone()


  }


  //utility function to initiate Grant process when a process ha no more outgoing requests
  private def grant(): Unit = {
    free=true
    in.foreach(sid =>
      processRecord.map.get(sid).get ! Grant(id)
    )
  }

  //utility function to initiate Notify process when a process first receives a Notify message
  private def notifyProc(): Unit = {

    notifyFlag=true
    out.foreach(sid =>
      processRecord.map(sid) ! Notify(id)
    )

    if (requests==0){
      logger.debug("No outgoing request at process "+id+" sending grants")
      grant()
    }
  }


  // utility function that checks if a process is good to send done to process it got message from
  private def sendDone(){
    if(done==0){
      logger.debug("Process "+id+" sending out done")
      while(neighborNotify.nonEmpty){
        val sid = neighborNotify.pop()
        processRecord.map(sid) ! Done
      }
      if(initiator){
        if(free){
          logger.debug("No Deadlock found")
        }else {
          logger.debug("Process is Deadlocked!!")
        }
        processRecord.map(-1) ! TerminateSystem
      }
    }
  }

  // utility function that checks if a process is good to send ack
  private def sendAck(): Unit = {
    if(ack==0){
      println("ack 0 at process "+id+" sending out acknowledgements")
      while(neighborGrant.nonEmpty){
        val sid = neighborGrant.pop()
        processRecord.map.get(sid).get ! Ack
      }
    }
  }


}
