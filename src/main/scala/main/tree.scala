//package main
//
//import akka.actor.{Actor, ActorRef, ActorSystem, Props}
//import akka.event.slf4j.Logger
//
//import scala.collection.mutable
//import scala.concurrent.Await
//import scala.concurrent.duration._
//import scala.io.Source
//
///**
// * Messages used in the tree algorithm.
// */
//case class Wave()
//case class Info()
//case class Decide()
//case object TreeTerminate
//
///**
// * Terminator actor responsible for terminating the actor system when the algorithm is complete.
// *
// * @param system The actor system to terminate.
// */
//class TreeTerminator(system: ActorSystem) extends Actor {
//  val log = Logger(getClass.getName)
//
//  override def receive: Receive = {
//    case TreeTerminate =>
//      log.info("Terminator received Terminate message")
//      system.terminate()
//  }
//}
//
///**
// * TreeProcess actor representing a node in the tree topology.
// *
// * @param id        The unique identifier of the process.
// * @param neighbors The list of neighboring process IDs.
// */
//class TreeProcess(val id: Int, val neighbors: List[Int]) extends Actor {
//  val log = Logger(getClass.getName)
//
//  // Map to keep track of received messages from neighbors
//  var received: Map[Int, Boolean] = neighbors.map(_ -> false).toMap
//  // Optional parent node
//  var parent: Option[Int] = None
//
//  log.info(s"[$self] initialized with neighbors: $neighbors")
//
//  def receive: Receive = {
//    case Wave =>
//      val senderId = getSender(context.sender())
//      received += (senderId -> true)
//      log.info(s"[$self] received Wave from [${context.sender()}]")
//      sendWave()
//
//    case Info =>
//      val senderId = getSender(context.sender())
//      log.info(s"[$self] received Info from [${context.sender()}]")
//      if (parent.contains(senderId)) {
//        // Forward Info message to other neighbors except the parent
//        neighbors.foreach { neighbor =>
//          if (neighbor != senderId) {
//            log.info(s"[$self] sending Info to [${TreeProcessRecord.map(neighbor)}]")
//            TreeProcessRecord.map(neighbor) ! Info
//          }
//        }
//      }
//
//    case Decide =>
//      log.info(s"[$self] decided")
//      // Send Terminate message to the Terminator actor
//      TreeProcessRecord.map(-1) ! TreeTerminate
//  }
//
//  /**
//   * Method to send Wave messages to unreceived neighbors.
//   * If there is only one unreceived neighbor, it becomes the parent and a Wave message is sent to it.
//   * If there are no unreceived neighbors, the process decides and sends Info messages to all neighbors except the parent.
//   */
//  def sendWave(): Unit = {
//    val unreceivedNeighbors = neighbors.filterNot(received)
//    if (unreceivedNeighbors.size == 1) {
//      val neighbor = unreceivedNeighbors.head
//      log.info(s"[$self] sending Wave to [${TreeProcessRecord.map(neighbor)}]")
//      TreeProcessRecord.map(neighbor) ! Wave
//      parent = Some(neighbor)
//      log.info(s"[$self] set [${TreeProcessRecord.map(neighbor)}] as parent")
//    } else if (unreceivedNeighbors.isEmpty) {
//      log.info(s"[$self] sending Decide to itself")
//      self ! Decide
//      // Send Info message to all neighbors except the parent
//      neighbors.foreach { neighbor =>
//        if (neighbor != parent.getOrElse(-1)) {
//          log.info(s"[$self] sending Info to [${TreeProcessRecord.map(neighbor)}]")
//          TreeProcessRecord.map(neighbor) ! Info
//        }
//      }
//    }
//  }
//
//  /**
//   * Utility method to get the sender ID from the ActorRef.
//   *
//   * @param actorRef The ActorRef of the sender.
//   * @return The sender ID if found, or -1 if not found.
//   */
//  def getSender(actorRef: ActorRef): Int = {
//    TreeProcessRecord.map.find(_._2 == actorRef).map(_._1).getOrElse(-1)
//  }
//}
//
///**
// * Object to store the mapping between process IDs and their corresponding ActorRefs.
// */
//object TreeProcessRecord {
//  val map: mutable.Map[Int, ActorRef] = mutable.Map.empty
//}
//
///**
// * Main object to run the tree algorithm.
// */
//object TreeAlgorithm {
//  val log = Logger(getClass.getName)
//
//  def main(): Unit = {
//    val system = ActorSystem("TreeAlgorithm")
//
//    // Read the tree topology from the text file
//    val filename = "/Users/dhruv/Desktop/CS553-DistributedAlgorithms/src/main/resources/NetGraph_30-03-24-18-54-55.ngs.dot"
//    val topologyLines = Source.fromFile(filename).getLines().toList
//
//    // Parse the topology and create a map of process IDs and their neighbors
//    val processConfig: Map[Int, List[Int]] = topologyLines
//      .filter(line => line.contains("->"))
//      .flatMap { line =>
//        val parts = line.split("->")
//        if (parts.length == 2) {
//          val from = parts(0).trim.replaceAll("\"", "").toInt
//          val to = parts(1).split("\\[")(0).trim.replaceAll("\"", "").toInt // Remove weight attribute
//          List((from, to), (to, from))
//        } else {
//          List.empty
//        }
//      }
//      .groupBy(_._1)
//      .mapValues(_.map(_._2).toList)
//      .toMap
//
//    // Create the actors dynamically based on the topology
//    processConfig.foreach { case (id, neighbors) =>
//      val process = system.actorOf(Props(new TreeProcess(id, neighbors)), s"process$id")
//      TreeProcessRecord.map += (id -> process)
//    }
//
//    // Loading Terminator process to initiate the termination once algorithm is complete
//    val terminator = system.actorOf(Props(new TreeTerminator(system)), "terminator")
//    TreeProcessRecord.map.put(-1, terminator)
//
//    log.info("Sending initial Wave messages")
//    // Find the leaf nodes (nodes with only one neighbor) and send initial Wave messages to them
//    val leafNodes = processConfig.filter { case (_, neighbors) => neighbors.size == 1 }.keys
//    leafNodes.foreach(id => TreeProcessRecord.map(id) ! Wave)
//
//    // Wait for the algorithm to terminate or timeout after 30 seconds
//    Await.ready(system.whenTerminated, 30.seconds)
//    log.info("Algorithm terminated")
//  }
//}