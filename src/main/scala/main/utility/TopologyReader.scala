package main.utility

import scala.io.Source

object TopologyReader {
  def readTopology(filename: String): Map[String, List[String]] = {
    val topologyLines = Source.fromFile(filename).getLines().toList

    topologyLines
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
  }
}