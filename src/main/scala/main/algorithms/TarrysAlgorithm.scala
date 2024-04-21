package main.algorithms

import akka.actor.{ActorSystem, Props}
import akka.event.slf4j.Logger
import main.processes.TarryProcess
import main.utility.{InitiateTarry, MessageTypes, ProcessRecord, Terminator, ApplicationProperties}

import scala.concurrent.Await
import scala.concurrent.duration._

object TarrysAlgorithm extends MessageTypes {
  val log = Logger(getClass.getName)

  def main(): Unit = {
    val system = ActorSystem("TarrysAlgorithm")
    val processRecord = new ProcessRecord

    val filename = ApplicationProperties.tarryInputFile
    val topologyLines = scala.io.Source.fromFile(filename).getLines().toList

    val processConfig: Map[Int, List[Int]] = topologyLines
      .filter(line => line.contains("->"))
      .flatMap { line =>
        val parts = line.split("->")
        if (parts.length == 2) {
          val from = parts(0).trim.replaceAll("\"", "").toInt
          val to = parts(1).split("\\[")(0).trim.replaceAll("\"", "").toInt
          List((from, to), (to, from))
        } else {
          List.empty
        }
      }
      .groupBy(_._1)
      .mapValues(_.map(_._2).toList)
      .toMap

    processConfig.foreach { case (id, neighbors) =>
      val initiator = id == 1
      val process = system.actorOf(Props(new TarryProcess(id, neighbors, initiator, processRecord)), s"process$id")
      processRecord.map += (id -> process)
    }

    val terminator = system.actorOf(Props(new Terminator(system)), "terminator")
    processRecord.map += (-1 -> terminator)

    log.info("Initiating the algorithm")
    processRecord.map.get(1).foreach(_ ! InitiateTarry)

    Await.ready(system.whenTerminated, 30.seconds)
    log.info("Algorithm terminated")
  }
}