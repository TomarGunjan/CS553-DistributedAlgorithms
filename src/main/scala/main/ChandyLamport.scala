package cs553.project

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

import scala.util.Success
import scala.util.Failure
import java.io.File
import java.util.Scanner
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import akka.pattern._
import akka.util.Timeout
import cs553.project.ChandyLamport.process1

import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext.Implicits.global

object ChandyLamport extends App {

  private object ProcessRecord {
    val map: mutable.Map[Int, ActorRef] = mutable.Map.empty
  }

  val system = ActorSystem("ChandyLamport")
//  implicit val timeout = Timeout(1.seconds)

  private case object PrintNeighbors
  private case object InitiateSnapshot
  private case object Marker
  private case class SendMessage(neighbor: ActorRef, action: String)
  private case object IncrementStoredVariable
  private case object DecrementStoredVariable
  private case object ResetVariables

  case class MessageRecord(process: ActorRef, MessageType: String) {}

  private class ChandyLamportActor(val id: Int, val neighbors: List[Int]) extends Actor {
    private var storedVariable = 0
    private var isMarked = false
//    var snapshotInitiated = false
    private val messageQueue = ListBuffer[MessageRecord]()
    private val ignoreMessagesFrom = ListBuffer[ActorRef]()

    override def receive: Receive = {
      case InitiateSnapshot =>
        // 1. Record your state
        println(s"Value of storedVariable = $storedVariable being recorded in snapshot of process $id")

        // 2. get neighbors from map and send marker message to those neighbors. Start recording messages on all incoming channels
        this.isMarked = true
        neighbors.foreach(neighbor => ProcessRecord.map(neighbor) ! Marker)
      case Marker =>
        // If this is the first marker message...
        if(!this.isMarked) {
          println(self + " received first marker")
          // Flag the variable
          this.isMarked = true

          // Record the state
          println(s"Value of storedVariable = $storedVariable being recorded in snapshot of process $id")
          // send marker on all outgoing channels
          neighbors.foreach(neighbor => ProcessRecord.map(neighbor) ! Marker)
        }

        // Ignore all further incoming messages from senders id
        println(self + " will ignore messages from " + sender)
        ignoreMessagesFrom += sender
      case SendMessage(target, message) =>
        if(message.equals("increment")) {
          target ! IncrementStoredVariable
        } else if(message.equals("decrement")) {
          target ! DecrementStoredVariable
        }
      case IncrementStoredVariable =>
        if(this.isMarked) {
          if(!ignoreMessagesFrom.contains(sender)) {
            println("STORING MESSAGE FROM MESSAGE QUEUE")
            messageQueue += MessageRecord(sender, "Increment")
          } else {
            val senderId = ProcessRecord.map.find(_._2 == sender).map(_._1).get
            println(s"MESSAGE REJECTED: Recording stopped on channel from process $senderId to process $id")
          }
        } else {
          storedVariable += 1
          println(s"Value of stored variable in process $id =  $storedVariable")
        }
      case DecrementStoredVariable =>
        if (this.isMarked) {
          if (!ignoreMessagesFrom.contains(sender)) {
            messageQueue += MessageRecord(sender, "Decrement")
          } else {
            val senderId = ProcessRecord.map.find(_._2 == sender).map(_._1).get
            println(s"MESSAGE REJECTED: Recording stopped on channel from process $senderId to process $id")
          }
        } else {
          storedVariable -= 1
          println(s"Value of stored variable in process $id =  $storedVariable")
        }
      case ResetVariables =>
        println("Reset")
      case PrintNeighbors =>
        neighbors.foreach(println)
    }
  }

  // CHANDY LAMPORT ALGORITHM

  // 0. Every process is connected to every other process.

  val process0 = system.actorOf(Props(new ChandyLamportActor(0, List(1, 2))), "process0")
  val process1 = system.actorOf(Props(new ChandyLamportActor(1, List(0, 2))), "process1")
  val process2 = system.actorOf(Props(new ChandyLamportActor(2, List(0, 1))), "process2")

  ProcessRecord.map ++= Map(
    0 -> process0,
    1 -> process1,
    2 -> process2
  )

  // 1. select random process from the map as initiator (later)

  println("Starting Snapshot initiation...")
  process1 ! IncrementStoredVariable
  process0 ! InitiateSnapshot
  process1 ! SendMessage(process2, "increment")
  process1 ! SendMessage(process2, "increment")
  process1 ! SendMessage(process2, "increment")
  process1 ! SendMessage(process2, "increment")
  process1 ! SendMessage(process2, "increment")

//  process1 ! SendMessage(process2, "increment")
//  process0 ! SendMessage(process2, "increment")

  // 3. If process receives marker -->
    // 3.1 if first marker -->
      // 3.1.1 record state
      // 3.1.2 Reject all messages from the channel which sent the marker
      // 3.1.3 send marker on all outgoing channel
      // 3.1.4 record messages on all incoming channels
    // 3.2 Stop recording on the channel that sent the marker

  // 4. Show snapshot internals --> variable state + stored messages.

  system.terminate()

}
