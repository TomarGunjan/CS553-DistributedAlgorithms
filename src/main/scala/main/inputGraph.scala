import scala.io.Source

case class Edge(from: String, to: String)

object DotParser {
  def parseConnections(dotFile: String): Seq[(String, Seq[String])] = {
    val lines = Source.fromFile(dotFile).getLines().toList
    val edgePattern = """^"(\w+)" -> "(\w+)" \[.*\]$""".r

    var connections = Map[String, Seq[String]]()

    lines.foreach {
      case edgePattern(from, to) =>
        connections += from -> (connections.getOrElse(from, Seq()) :+ to)
        connections += to -> (connections.getOrElse(to, Seq()) :+ from)
      case _ => // Ignore other lines
    }

    connections.toSeq
  }
}

object Main {
  def main(args: Array[String]): Unit = {
    val dotFile = "/Users/dhruv/Desktop/cs441/NetGameSim/NetGraph_30-03-24-18-54-55.ngs.dot"
    val connections = DotParser.parseConnections(dotFile)
    println("Connections:")
    connections.foreach { case (node, connectedNodes) =>
      println(s"$node is connected to ${connectedNodes.mkString(", ")}")
    }
  }
}
