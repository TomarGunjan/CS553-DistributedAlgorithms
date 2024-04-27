package main.utility

import akka.actor.ActorRef


/**
 * All the Message types are listed here as case classes
 */
trait MessageTypes

case class Initiate() extends MessageTypes
case class Notify(id:Int) extends MessageTypes
case class Grant(id: Int) extends MessageTypes
case class Done() extends MessageTypes
case class Ack() extends MessageTypes
case class TerminateProcess(processRecord: ProcessRecord) extends MessageTypes
case class TerminateSystem() extends MessageTypes

case class InitiateTarry() extends MessageTypes
case class TarryProbe(id: Int) extends MessageTypes
case object TerminateTarry extends MessageTypes

case class Wave() extends MessageTypes
case class AllProcessesCreated() extends MessageTypes
case class Info() extends MessageTypes
case class Decide() extends MessageTypes
case object TreeTerminate extends MessageTypes

case class EchoWave() extends MessageTypes
case class EchoTerminate() extends MessageTypes

case class InitiateSnapshot(start : Boolean) extends MessageTypes
//case class SendMessage(target: ActorRef, messageBody: String) extends MessageTypes
case class SendMessage(messageBody: String) extends MessageTypes
case class PerformAction(messageBody: String) extends MessageTypes
case object InitiateSnapshotActors extends MessageTypes
case class InitiateSnapshotWithMessageCount(preSnapshotMessageCount: Int, start: Boolean) extends MessageTypes
case class PerformActionWithTagPayload(str: String, snapshotTaken: Boolean) extends MessageTypes
