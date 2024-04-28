import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import main.processes.EchoProcess
import main.utility.{EchoWave, EchoTerminate, AllProcessesCreated, ProcessRecord}

/**
 * Test suite for the EchoProcess actor.
 * Extends the TestKit class to provide an actor system for testing.
 * Mixes in ImplicitSender, AnyWordSpecLike, Matchers, and BeforeAndAfterAll traits.
 */
class EchoTest extends TestKit(ActorSystem("EchoTest"))
  with ImplicitSender
  with AnyWordSpecLike
  with Matchers
  with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "EchoProcess" should {
    "send Wave messages to neighbors and terminate when initiated" in {
      val processRecord = new ProcessRecord

      // Create EchoProcess actors based on the topology
      val process1 = system.actorOf(Props(new EchoProcess("1", List("2"), initiator = true, processRecord)), "process1_test1")
      val process2 = system.actorOf(Props(new EchoProcess("2", List("1"), initiator = false, processRecord)), "process2_test1")

      // Set up the ProcessRecord
      processRecord.map ++= Map(1 -> process1, 2 -> process2)

      // Create a TestProbe for the terminator
      val terminatorProbe = TestProbe()
      processRecord.map += (-1 -> terminatorProbe.ref)

      // Send AllProcessesCreated message to the initiator (process1)
      process1 ! AllProcessesCreated

      // Verify that process2 receives the EchoWave message
      process2.tell(EchoWave(), process1)

      // Send EchoWave message back to process1 from process2
      process1.tell(EchoWave(), process2)

      // Verify that the terminator receives the EchoTerminate message
      terminatorProbe.expectMsg(EchoTerminate())
    }
    "perform echo algorithm in a fully connected topology" in {
      val processRecord = new ProcessRecord

      // Create EchoProcess actors based on the topology
      val process1 = system.actorOf(Props(new EchoProcess("1", List("2", "3", "4"), initiator = true, processRecord)), "process1_test4")
      val process2 = system.actorOf(Props(new EchoProcess("2", List("1", "3", "4"), initiator = false, processRecord)), "process2_test4")
      val process3 = system.actorOf(Props(new EchoProcess("3", List("1", "2", "4"), initiator = false, processRecord)), "process3_test4")
      val process4 = system.actorOf(Props(new EchoProcess("4", List("1", "2", "3"), initiator = false, processRecord)), "process4_test4")

      // Set up the ProcessRecord
      processRecord.map ++= Map(1 -> process1, 2 -> process2, 3 -> process3, 4 -> process4)

      // Create a TestProbe for the terminator
      val terminatorProbe = TestProbe()
      processRecord.map += (-1 -> terminatorProbe.ref)

      // Send AllProcessesCreated message to the initiator (process1)
      process1 ! AllProcessesCreated

      // Verify that the terminator receives the EchoTerminate message
      terminatorProbe.expectMsg(EchoTerminate())
    }
    "perform echo algorithm in a ring topology" in {
      val processRecord = new ProcessRecord

      // Create EchoProcess actors based on the topology
      val process1 = system.actorOf(Props(new EchoProcess("1", List("2", "5"), initiator = true, processRecord)), "process1_test5")
      val process2 = system.actorOf(Props(new EchoProcess("2", List("1", "3"), initiator = false, processRecord)), "process2_test5")
      val process3 = system.actorOf(Props(new EchoProcess("3", List("2", "4"), initiator = false, processRecord)), "process3_test5")
      val process4 = system.actorOf(Props(new EchoProcess("4", List("3", "5"), initiator = false, processRecord)), "process4_test5")
      val process5 = system.actorOf(Props(new EchoProcess("5", List("4", "1"), initiator = false, processRecord)), "process5_test5")

      // Set up the ProcessRecord
      processRecord.map ++= Map(1 -> process1, 2 -> process2, 3 -> process3, 4 -> process4, 5 -> process5)

      // Create a TestProbe for the terminator
      val terminatorProbe = TestProbe()
      processRecord.map += (-1 -> terminatorProbe.ref)

      // Send AllProcessesCreated message to the initiator (process1)
      process1 ! AllProcessesCreated

      // Verify that the terminator receives the EchoTerminate message
      terminatorProbe.expectMsg(EchoTerminate())
    }
  }
}
