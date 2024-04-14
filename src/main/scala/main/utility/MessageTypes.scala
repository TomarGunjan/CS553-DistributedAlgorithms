package main.utility

trait MessageTypes

case class Initiate() extends MessageTypes
case class Notify(id:Int) extends MessageTypes
case class Grant(id: Int) extends MessageTypes
case class Done() extends MessageTypes
case class Ack() extends MessageTypes


