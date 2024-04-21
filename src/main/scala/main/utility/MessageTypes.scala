package main.utility


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
case class Info() extends MessageTypes
case class Decide() extends MessageTypes
case object TreeTerminate extends MessageTypes

case class EchoWave() extends MessageTypes
case class EchoTerminate() extends MessageTypes

