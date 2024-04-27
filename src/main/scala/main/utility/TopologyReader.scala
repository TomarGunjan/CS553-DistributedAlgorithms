package main.utility

import scala.io.Source

/**
 * Object for reading and processing topology files.
 */
object TopologyReader {
  /**
   * Reads a topology file and returns a map representing the process configuration.
   *
   * @param filename The name of the topology file.
   * @return A map where the keys are process IDs and the values are lists of neighboring process IDs.
   */
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

  /**
   * Checks if the process configuration contains a cycle.
   *
   * @param processConfig The process configuration map.
   * @return True if a cycle is detected, false otherwise.
   */
  def hasCycle(processConfig: Map[String, List[String]]): Boolean = {
    val visited = collection.mutable.Set[String]()

    /**
     * Performs a depth-first search (DFS) traversal to detect cycles.
     *
     * @param node   The current node being visited.
     * @param parent The parent node of the current node.
     * @return True if a cycle is detected, false otherwise.
     */
    def dfs(node: String, parent: String): Boolean = {
      visited += node

      processConfig(node).foreach { neighbor =>
        if (neighbor == parent) {
          // Skip the parent node to avoid false-positive cycle detection
        } else if (visited.contains(neighbor)) {
          // Found a cycle if the neighbor is already visited
          return true
        } else {
          // Recursively visit the neighbor
          if (dfs(neighbor, node)) return true
        }
      }

      false
    }

    processConfig.keys.exists { node =>
      if (!visited.contains(node)) dfs(node, "")
      else false
    }
  }
}
