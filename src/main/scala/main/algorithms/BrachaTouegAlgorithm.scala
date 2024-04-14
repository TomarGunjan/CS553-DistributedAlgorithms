package main.algorithms

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.event.slf4j.Logger
import main.processes.BrachaTouegProcess
import main.utility.{Initiate,  ProcessRecord,  Terminator}
import scala.language.postfixOps



object BrachaTouegAlgorithm{
  val logger = Logger(getClass.getName)
  val system = ActorSystem("BrachaTouegSystem")
  val record = new ProcessRecord()
  def main(){

    logger.info("Starting the Algorithm Demo")
    logger.info("Case 1 : Demonstrating a case where the initiator is not deadlocked")

    val process0 = system.actorOf(Props(new BrachaTouegProcess(0, system, List(1,3),List(), true, record )), name = "process0")
    val process1 = system.actorOf(Props(new BrachaTouegProcess(1, system, List(2),List(0), false,record )), name = "process1")
    val process2 = system.actorOf(Props(new BrachaTouegProcess(2, system, List(3),List(1),false, record  )), name = "process2")
    val process3 = system.actorOf(Props(new BrachaTouegProcess(3, system, List(0),List(0,2),false, record )), name = "process3")
    val terminator = system.actorOf(Props(new Terminator(system)))

    record.map.put(0, process0)
    record.map.put(1, process1)
    record.map.put(2,process2)
    record.map.put(3,process3)
    record.map.put(-1,terminator)

    process0 ! Initiate
  }





}
