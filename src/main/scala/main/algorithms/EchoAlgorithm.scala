package main.algorithms

import akka.actor.{ActorSystem, Props}
import akka.event.slf4j.Logger
import main.processes.EchoProcess
import main.utility.{MessageTypes, Terminator, ApplicationProperties}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.Source

object EchoAlgorithm extends MessageTypes {
  val log = Logger(getClass.getName)

  def main(): Unit = {
    val system = ActorSystem("EchoAlgorithmSystem")

    val filename = ApplicationProperties.echoInputFile
    val topologyLines = Source.fromFile(filename).getLines().toList

    val processConfig: Map[String, List[String]] = topologyLines
      .filter(line => line.contains("->"))
      .flatMap { line =>
        val parts = line.split("->")
        if (parts.length == 2) {
          val from = parts(0).trim.replaceAll("\"", "")
          val to = parts(1).split("\\[")(0).trim.replaceAll("\"", "")
          List((from, to), (to, from))
        } else {
          List.empty
        }
      }
      .groupBy(_._1)
      .mapValues(_.map(_._2).toList)
      .toMap

    processConfig.foreach { case (id, neighbors) =>
      val initiator = id == "1"
      val process = system.actorOf(Props(new EchoProcess(id, neighbors, initiator)), id)
    }

    val terminator = system.actorOf(Props(new Terminator(system)), "terminator")

    Await.ready(system.whenTerminated, 30.seconds)
    log.info("Algorithm terminated")
  }
}