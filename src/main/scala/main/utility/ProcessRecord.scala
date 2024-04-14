package main.utility

import akka.actor.ActorRef

import scala.collection.mutable

class ProcessRecord {
  val map = mutable.Map.empty[Int, ActorRef]
}
