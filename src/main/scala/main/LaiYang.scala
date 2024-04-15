package cs553.project

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object LaiYang extends App {

  private object ProcessRecord {
    val map: mutable.Map[Int, ActorRef] = mutable.Map.empty
  }

  val system = ActorSystem("LaiYang")

  private case object InitiateSnapshot
  private case class TakeSnapshot(numberOfFalseMessages: Int)
  private case class SendMessage(neighbor: ActorRef, action: String)
  private case class SendMessageWithTag(neighbor: ActorRef, action: String, tag: Boolean)
  private case class ProcessMessage(action: String, tag: Boolean)
  private case object IncrementStoredVariable
  private case object DecrementStoredVariable

  case class MessageRecord(process: ActorRef, MessageType: String) {}

  private class LaiYangActor(val id: Int, val neighbors: List[Int], var snapshotTaken: Boolean) extends Actor {

    private var storedVariable = 0
    private var storedVariableInSnapshot = 0
    private val messageQueue = ListBuffer[MessageRecord]()
    private val numberOfFalseMessagesSent: mutable.Map[ActorRef, Int] = mutable.Map.empty
    private val numberOfFalseMessagesReceived: mutable.Map[ActorRef, Int] = mutable.Map.empty

    override def receive: Receive = {
      case InitiateSnapshot =>
        this.snapshotTaken = true;
        neighbors.foreach(neighbor => ProcessRecord.map(neighbor) ! TakeSnapshot(numberOfFalseMessagesSent(ProcessRecord.map(neighbor))))
      case TakeSnapshot(numberOfFalseMessages) =>
        numberOfFalseMessagesReceived += sender -> (numberOfFalseMessages - numberOfFalseMessagesReceived(sender))
        self ! InitiateSnapshot
        println()
      case SendMessage(neighbor, action) =>
        if (!this.snapshotTaken) numberOfFalseMessagesSent += neighbor -> (numberOfFalseMessagesSent(neighbor) + 1)
        neighbor ! ProcessMessage(action, this.snapshotTaken)
      case ProcessMessage(action, tag) =>
        if (!this.snapshotTaken && tag) numberOfFalseMessagesReceived += sender -> (numberOfFalseMessagesReceived(sender) + 1)

        if(this.snapshotTaken && !tag) numberOfFalseMessagesReceived += sender -> (numberOfFalseMessagesReceived(sender) - 1)

        if (tag && !this.snapshotTaken) {
          self ! InitiateSnapshot
        }

        if(this.snapshotTaken && !tag && numberOfFalseMessagesReceived(sender) >= 0) messageQueue += MessageRecord(sender, action)

        if(action.equals("increment")) {
          self ! IncrementStoredVariable
        } else if(action.equals("decrement")) {
          self ! DecrementStoredVariable
        }

      case IncrementStoredVariable =>
        storedVariable += 1
      case DecrementStoredVariable =>
        storedVariable -= 1
    }
  }

  val process0 = system.actorOf(Props(new LaiYangActor(0, List(1, 2), false)), "process0")
  val process1 = system.actorOf(Props(new LaiYangActor(1, List(0, 2), false)), "process1")
  val process2 = system.actorOf(Props(new LaiYangActor(2, List(0, 1), false)), "process2")

  ProcessRecord.map ++= Map(
    0 -> process0,
    1 -> process1,
    2 -> process2
  )

}
