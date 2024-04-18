package main.utility

import akka.actor.ActorRef

import scala.collection.mutable

/**
 * Utility class to record Process against their ids for easy access
 */
class ProcessRecord {
  val map = mutable.Map.empty[Int, ActorRef]
}
