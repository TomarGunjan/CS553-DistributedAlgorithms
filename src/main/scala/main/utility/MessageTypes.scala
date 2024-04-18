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


