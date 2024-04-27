package main

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import main.processes.EchoProcess
import main.utility.{EchoWave, EchoTerminator}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class EchoProcessSpec extends TestKit(ActorSystem("EchoProcessSpec"))
  with ImplicitSender
  with AnyFlatSpecLike
  with Matchers
  with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "EchoProcess" should "forward EchoWave to neighbors when not initiator and parent not set" in {
    val process = system.actorOf(Props(new EchoProcess("1", List("2", "3"), initiator = false)))
    process ! EchoWave()
    expectMsgAllOf(EchoWave(), EchoWave())
  }

  it should "send EchoWave back to sender when not initiator and only one neighbor" in {
    val process = system.actorOf(Props(new EchoProcess("1", List("2"), initiator = false)))
    process ! EchoWave()
    expectMsg(EchoWave())
  }

  it should "send EchoWave to parent when received from all neighbors" in {
    val process = system.actorOf(Props(new EchoProcess("1", List("2", "3"), initiator = false)))
    process ! EchoWave() // Set parent
    process ! EchoWave() // Receive from neighbor 1
    process ! EchoWave() // Receive from neighbor 2
    expectMsg(EchoWave()) // Expect sending to parent
  }

  it should "decide and terminate when initiator and received from all neighbors" in {
    val terminator = system.actorOf(Props(new EchoTerminator(system)), "terminator")
    val process = system.actorOf(Props(new EchoProcess("1", List("2", "3"), initiator = true)))
    process ! EchoWave() // Receive from neighbor 1
    process ! EchoWave() // Receive from neighbor 2
    expectMsg("terminate") // Expect terminator to receive "terminate" message
  }
}