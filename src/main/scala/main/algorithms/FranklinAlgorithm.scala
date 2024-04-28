package main.algorithms

import akka.actor.{ActorSystem, Props}
import akka.event.slf4j.Logger
import com.typesafe.config.Config
import main.processes.{FranklinProcess}
import main.utility.{ApplicationProperties, Initiate, FranklinOrchestrator, ProcessRecord, Terminator}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

import scala.language.postfixOps


/**
 * Object representing Franklin algorithm.
 */
object FranklinAlgorithm {
  // Logger for logging messages
  val logger = Logger(getClass.getName)

  /**
   * Main entry point of Franklin algorithm.
   * Initializes the actor system, loads the test data , creates the actors,
   * and runs the algorithm.
   */
  def main(): Unit = {
    val system = ActorSystem("FranklinSystem")
    logger.info("ActorSystem Franklin Initialized!")

    // Creating a new ProcessRecord to store the mapping of process IDs to actor references
    val record = new ProcessRecord()
    //loading test data
    createTestData(ApplicationProperties.franklinData,record,system)
    logger.info("Test Data loaded")

    //Initiating algorithm
    logger.info("Initiating Orchestrator for starting algorithm")
    record.map.get(-2).get ! Initiate

    Await.ready(system.whenTerminated, 30.seconds)
    logger.info("Terminating the algorithm")

  }

// This function loads test data
  def createTestData(data : List[Config], record : ProcessRecord, system : ActorSystem ): Unit = {
    for(nodeData<-data){
      val id = nodeData.getInt("id")
      val leftNeighbour = nodeData.getInt("leftNeighbor")
      val rightNeighbour = nodeData.getInt("rightNeighbor")
      val Process = system.actorOf(Props(new FranklinProcess(id,leftNeighbour, rightNeighbour,system, record)), name = "process"+ id)
      logger.info("Process"+id+" created.")
      record.map.put(id, Process);
    }

    val terminator = system.actorOf(Props(new Terminator(system)))
    record.map.put(-1, terminator)

    val processes = record.map.keys.filter(_ >= 0).toList
    val orchestrator = system.actorOf(Props(new FranklinOrchestrator(processes,system,record)))
    record.map.put(-2, orchestrator)

  }
}