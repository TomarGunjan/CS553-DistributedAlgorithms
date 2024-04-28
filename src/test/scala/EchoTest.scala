import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import main.processes.EchoProcess
import main.utility.{AllProcessesCreated, EchoTerminate, ProcessRecord}

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

  /**
   * Shutdown the actor system after all tests have finished.
   */
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "EchoProcess" should {
    "perform echo algorithm and terminate when the initiator receives echo messages from all neighbors" in {
      val processRecord = new ProcessRecord

      // Create EchoProcess actors based on the topology
      val process1 = system.actorOf(Props(new EchoProcess(1, List(2, 3), initiator = true, processRecord)), "1")
      val process2 = system.actorOf(Props(new EchoProcess(2, List(1), initiator = false, processRecord)), "2")
      val process3 = system.actorOf(Props(new EchoProcess(3, List(1), initiator = false, processRecord)), "3")

      // Set up the ProcessRecord
      processRecord.map ++= Map(1 -> process1, 2 -> process2, 3 -> process3)

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