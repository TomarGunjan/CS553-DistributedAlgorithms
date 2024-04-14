package main.brachaToueg

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

import scala.collection.mutable
import scala.language.postfixOps


case class Initiate()
case class Notify(id:Int)
case class Grant(id: Int)
case class Done()
case class Ack()

class BrachaTouegProcess(val id: Int, val system: ActorSystem, val out: List[Int], val in : List[Int], val initiator:Boolean) extends Actor {
  var notifyFlag = false
  var free=false
  var requests = out.size
  var done=out.size
  var ack = in.size
  var neighborNotify = mutable.Stack[Int]()
  var neighborGrant = mutable.Stack[Int]()


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
        ProcessRecord.map.get(nid).get ! Done
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
          ProcessRecord.map.get(nid).get ! Ack
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
          ProcessRecord.map.get(sid).get ! Ack
        }
      }
      sendDone()


  }

  private def grant(): Unit = {
    free=true
    in.foreach(sid =>
      ProcessRecord.map.get(sid).get ! Grant(id)
    )
  }

  private def notifyProc(): Unit = {

    notifyFlag=true
    out.foreach(sid =>
      ProcessRecord.map(sid) ! Notify(id)
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
        ProcessRecord.map.get(sid).get ! Done
      }
      if(initiator){
        if(free){
          println("No Deadlock found")
        }else {
          println("Process id deadlocked")
        }

      }
    }
  }


}

object ProcessRecord{
  val map = mutable.Map.empty[Int, ActorRef]
}

object BrachaTouegAlgorithm2 extends App {
  val system = ActorSystem("BrachaTouegSystem")
  val process0 = system.actorOf(Props(new BrachaTouegProcess(0, system, List(1,3),List(), true)), name = "process0")
  val process1 = system.actorOf(Props(new BrachaTouegProcess(1, system, List(2),List(0), false)), name = "process1")
  val process2 = system.actorOf(Props(new BrachaTouegProcess(2, system, List(3),List(1),false)), name = "process2")
  val process3 = system.actorOf(Props(new BrachaTouegProcess(3, system, List(),List(0,2),false)), name = "process3")


  ProcessRecord.map.put(0, process0)
  ProcessRecord.map.put(1, process1)
  ProcessRecord.map.put(2,process2)
  ProcessRecord.map.put(3,process3)

  process0 ! Initiate
  while(process0.)
}
