import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import main.processes.BrachaTouegProcess
import main.utility.{Ack, Done, Grant, Initiate, Notify, ProcessRecord, Terminator}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration.DurationInt

class BrachaTouegProcessSpec extends TestKit(ActorSystem("BrachaTouegProcessSpec"))
  with AnyWordSpecLike
  with Matchers
  with BeforeAndAfterAll
  with ImplicitSender {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }
  "BrachaTouegProcess" should {

    "should send out Notify to neighbour when Initiated" in {
      // Create a ProcessRecord instance
      val processRecord = new ProcessRecord

      // Create instances of BrachaTouegProcess actors
      val process1: ActorRef = system.actorOf(Props(new BrachaTouegProcess(1, system, List(2, 3), List(2, 3), initiator = true, processRecord)))
      val process2 = TestProbe()
      val process3 = TestProbe()
      val terminator = system.actorOf(Props(new Terminator(system)))

      // Register actors in the ProcessRecord
      processRecord.map += (1 -> process1)
      processRecord.map += (2 -> process2.ref)
      processRecord.map += (3 -> process3.ref)
      processRecord.map.put(-1,terminator)

      // Send Initiate message to process1
      process1 ! Initiate

      // Expect Notify messages sent to neighbors of process1
      process2.expectMsg(Notify(1))
      process3.expectMsg(Notify(1))


    }


    "should send out Notify to neighbour when receives a Notify message" in {
      // Create a ProcessRecord instance
      val processRecord = new ProcessRecord

      // Create instances of BrachaTouegProcess actors
      val process1: ActorRef = system.actorOf(Props(new BrachaTouegProcess(1, system, List(2, 3), List(2, 3), initiator = true, processRecord)))
      val process2 = TestProbe()
      val process3 = TestProbe()
      val terminator = system.actorOf(Props(new Terminator(system)))

      // Register actors in the ProcessRecord
      processRecord.map += (1 -> process1)
      processRecord.map += (2 -> process2.ref)
      processRecord.map += (3 -> process3.ref)
      processRecord.map.put(-1,terminator)

      // Send Notify message to process1
      process1 ! Notify(0)

      // Expect Notify messages sent to neighbors of process1

      process2.expectMsg(Notify(1))
      process3.expectMsg(Notify(1))

    }

    "should send out Grant to neighbours when No outgoing request" in {
      // Create a ProcessRecord instance
      val processRecord = new ProcessRecord

      // Create instances of BrachaTouegProcess actors
      val process1: ActorRef = system.actorOf(Props(new BrachaTouegProcess(1, system, List(), List(2, 3), initiator = true, processRecord)))
      val process2 = TestProbe()
      val process3 = TestProbe()
      val terminator = system.actorOf(Props(new Terminator(system)))

      // Register actors in the ProcessRecord
      processRecord.map += (1 -> process1)
      processRecord.map += (2 -> process2.ref)
      processRecord.map += (3 -> process3.ref)
      processRecord.map.put(-1,terminator)

      // Send Initiate message to process1
      process1 ! Initiate

      // Expect Grant messages sent to Process 2 and 3

      process2.expectMsg(Grant(1))
      process3.expectMsg(Grant(1))

    }

    "should send out Done to parent all acknowledments receivedfor a node with no outgoing edges" in {
      // Create a ProcessRecord instance
      val processRecord = new ProcessRecord

      // Create instances of BrachaTouegProcess actors
      val process1 = TestProbe()
      val process2: ActorRef = system.actorOf(Props(new BrachaTouegProcess(2, system, List(), List(1), initiator = true, processRecord)))
      val terminator = system.actorOf(Props(new Terminator(system)))

      // Register actors in the ProcessRecord
      processRecord.map += (1 -> process1.ref)
      processRecord.map += (2 -> process2)
      processRecord.map.put(-1,terminator)

      // Send Notify message to process2
      process2 ! Notify(1)

      // Since process2 has no outgoing request. it will immediately grant request and Process1 receives Grant message
      process1.expectMsg(Grant(2))

      //Send Ack to Process2
      process2 ! Ack

      // Process2 has received acknowledgement from all incoming request process hence Process1 should receive Done from Process2
      process1.expectMsg(Done)
    }

    "should send out Ack to node it receioved Grant from" in {
      val processRecord = new ProcessRecord

      // Create instances of BrachaTouegProcess actors

      val process1: ActorRef = system.actorOf(Props(new BrachaTouegProcess(1, system, List(1), List(), initiator = true, processRecord)))
      val process2 = TestProbe()
      val terminator = system.actorOf(Props(new Terminator(system)))

      // Register actors in the ProcessRecord
      processRecord.map += (1 -> process1)
      processRecord.map += (2 -> process2.ref)
      processRecord.map.put(-1,terminator)

      // Send Grant Message to Process 2 from Process1
      process1 ! Grant(2)

      // Process1 should send Acknowledgement back to Process2
      process2.expectMsg(10.seconds, Ack)

    }

}

}
