package main.processes

import akka.actor.{Actor, ActorSystem}
import akka.event.slf4j.Logger
import main.utility.{Ack, Done, Grant, Initiate, Notify, ProcessRecord, Terminate}

import scala.collection.mutable

class BrachaTouegProcess(val id: Int, val system: ActorSystem, val out: List[Int], val in : List[Int], val initiator:Boolean, val processRecord: ProcessRecord) extends Actor {
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
    //neighbors.foreach(pid => context.watch(ProcessRecord.map.get(pid).get))
  }


  def receive = {
    case Initiate =>
      println("Initiated by process "+id)
      notifyProc()

    case Notify(nid) =>
      println("notify received at "+id+" from "+nid)
      if (!notifyFlag) {
        neighborNotify.push(nid)
        notifyProc()
      } else{
        processRecord.map.get(nid).get ! Done
      }





    case Grant(nid) =>
      println("grant received at "+id+" from "+nid)
      if (requests>0) {
        requests-=1
        if (requests==0){
          println("total request 0 at "+id+" executing grant procedures")
          neighborGrant.push(nid)
          grant()
        }else {
          processRecord.map.get(nid).get ! Ack
        }
      }



    case Done =>
      done-=1
      println("received done at process "+id+". Total ack left "+done)
      if (done==0){
        sendDone()
      }

    case Ack =>
      ack-=1
      println("received acknowledgement at process "+id+". Total ack left "+ack)
      if(ack==0){
        println("ack 0 at process "+id+" sending out acknowledgements")
        while(neighborGrant.nonEmpty){
          val sid = neighborGrant.pop()
          processRecord.map.get(sid).get ! Ack
        }
      }
      sendDone()


  }

  private def grant(): Unit = {
    free=true
    in.foreach(sid =>
      processRecord.map.get(sid).get ! Grant(id)
    )
  }

  private def notifyProc(): Unit = {

    notifyFlag=true
    out.foreach(sid =>
      processRecord.map(sid) ! Notify(id)
    )

    if (requests==0){
      println("no outgoing request at process "+id+" sending grants")
      grant()
    }
  }

  private def sendDone(){
    if(done==0){
      println("process "+id+" sending out done")
      while(neighborNotify.nonEmpty){
        val sid = neighborNotify.pop()
        processRecord.map(sid) ! Done
      }
      if(initiator){
        if(free){
          println("No Deadlock found")
        }else {
          println("Process id deadlocked")
        }
        processRecord.map(-1) ! Terminate
      }
    }
  }


}
