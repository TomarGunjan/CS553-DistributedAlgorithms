package main.algorithms

import akka.actor.{ActorSystem, Props}
import akka.event.slf4j.Logger
import main.processes.TreeProcess
import main.utility.{Wave, MessageTypes, ProcessRecord, Terminator, ApplicationProperties}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.Source

object TreeAlgorithm extends MessageTypes {
  val log = Logger(getClass.getName)

  def main(): Unit = {
    val system = ActorSystem("TreeAlgorithm")
    val processRecord = new ProcessRecord

    val filename = ApplicationProperties.treeInputFile
    val topologyLines = Source.fromFile(filename).getLines().toList

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
      val process = system.actorOf(Props(new TreeProcess(id, neighbors, processRecord)), s"process$id")
      processRecord.map += (id -> process)
    }

    val terminator = system.actorOf(Props(new Terminator(system)), "terminator")
    processRecord.map.put(-1, terminator)

    log.info("Sending initial Wave messages")
    val leafNodes = processConfig.filter { case (_, neighbors) => neighbors.size == 1 }.keys
    leafNodes.foreach(id => processRecord.map(id) ! Wave)

    Await.ready(system.whenTerminated, 30.seconds)
    log.info("Algorithm terminated")
  }
}