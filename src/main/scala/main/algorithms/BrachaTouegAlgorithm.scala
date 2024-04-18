package main.algorithms

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.event.slf4j.Logger
import com.typesafe.config.Config
import main.processes.BrachaTouegProcess
import main.utility.{ApplicationProperties, Initiate, ProcessRecord, TerminateSystem, Terminator}

import java.util
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters.ListHasAsScala
import scala.language.postfixOps



object BrachaTouegAlgorithm{
  val logger = Logger(getClass.getName)


  def main(){
    logger.info("Starting the Algorithm Demo")
    //Running Scenario 1 with No Deadlock
    scenario1NoDeadlock()
    //Running scenario 2 with Deadlock
    scenario2WithDeadlock()
  }


  def scenario1NoDeadlock(): Unit = {
    logger.info("Case 1 : Demonstrating a case where the initiator is not deadlocked")

    //initializing the Actor system using which Process will be made
    val system = ActorSystem("BrachaTouegSystem")
    logger.info("ActorSystem BrachaTouegInitialized !!")

    // initializing record to store Processes against their ids
    val record = new ProcessRecord()

    //fetching Config for test Data
    val testData = ApplicationProperties.deadlockFreeData

    // loading test data
    createTestData(testData,record, system)

    logger.info("Test Data Loaded")

    // Loading Terminator process to initiate the trmination once algorithm is complete
    val terminator = system.actorOf(Props(new Terminator(system)))
    record.map.put(-1,terminator)

    //  initiating algorithm
    logger.info("Initiating the Algorithm")
    record.map(testData.getInt("initiator")) ! Initiate

    Await.ready(system.whenTerminated, 30.seconds)
    logger.info("Terminating the algorithm")
  }



  def scenario2WithDeadlock(): Unit = {
    logger.info("Case 2 : Demonstrating a case where the initiator is deadlocked")
    //initializing the Actor system using which Process will be made
    val system = ActorSystem("BrachaTouegSystem")
    logger.info("ActorSystem BrachaTouegInitialized !!")

    // initializing record to store Processes against their ids
    val record = new ProcessRecord()

    //fetching Config for test Data
    val testData = ApplicationProperties.deadlockData

    // loading test data
    createTestData(testData,record, system)

    logger.info("Test Data Loaded")

    // Loading Terminator process to initiate the trmination once algorithm is complete
    val terminator = system.actorOf(Props(new Terminator(system)))
    record.map.put(-1,terminator)

    //  initiating algorithm
    logger.info("Initiating the Algorithm")
    record.map(testData.getInt("initiator")) ! Initiate

    Await.ready(system.whenTerminated, 30.seconds)
    logger.info("Terminating the algorithm")
  }



//Utility function to load test data
  def createTestData(data : Config, record : ProcessRecord, system : ActorSystem ): Unit = {
      for(nodeData:Config<-data.getConfigList("nodeData").asScala.toList){
        val id = nodeData.getInt("Node")
        val Process = system.actorOf(Props(new BrachaTouegProcess(nodeData.getInt("Node"), system, nodeData.getIntList("Out").asScala.toList,nodeData.getIntList("In").asScala.toList, id==data.getInt("initiator"), record )), name = "process"+ id)
        record.map.put(id, Process);
      }

  }


}
