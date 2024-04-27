import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import main.processes.TarryProcess
import main.utility.{InitiateTarry, TarryProbe, TerminateTarry, ProcessRecord}

/**
 * Test suite for the TarryProcess actor.
 * Extends the TestKit class to provide an actor system for testing.
 * Mixes in ImplicitSender, AnyWordSpecLike, Matchers, and BeforeAndAfterAll traits.
 */
class TarryTest extends TestKit(ActorSystem("TarryTest"))
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

  "TarryProcess" should {
    "perform token traversal and terminate when the initiator receives tokens through all channels" in {
      val processRecord = new ProcessRecord

      // Create TarryProcess actors based on the topology
      val process1 = system.actorOf(Props(new TarryProcess(1, List(2, 3), initiator = true, processRecord)), "process1_test1")
      val process2 = system.actorOf(Props(new TarryProcess(2, List(1), initiator = false, processRecord)), "process2_test1")
      val process3 = system.actorOf(Props(new TarryProcess(3, List(1), initiator = false, processRecord)), "process3_test1")

      // Set up the ProcessRecord
      processRecord.map ++= Map(1 -> process1, 2 -> process2, 3 -> process3)

      // Create a TestProbe for the terminator
      val terminatorProbe = TestProbe()
      processRecord.map += (-1 -> terminatorProbe.ref)

      // Send InitiateTarry message to the initiator (process1)
      process1 ! InitiateTarry

      // Verify that the terminator receives the TerminateTarry message
      terminatorProbe.expectMsg(TerminateTarry)
    }

    "perform token traversal and terminate for a linear topology" in {
      val processRecord = new ProcessRecord

      // Create TarryProcess actors based on the topology
      val process1 = system.actorOf(Props(new TarryProcess(1, List(2), initiator = true, processRecord)), "process1_test2")
      val process2 = system.actorOf(Props(new TarryProcess(2, List(1, 3), initiator = false, processRecord)), "process2_test2")
      val process3 = system.actorOf(Props(new TarryProcess(3, List(2), initiator = false, processRecord)), "process3_test2")

      // Set up the ProcessRecord
      processRecord.map ++= Map(1 -> process1, 2 -> process2, 3 -> process3)

      // Create a TestProbe for the terminator
      val terminatorProbe = TestProbe()
      processRecord.map += (-1 -> terminatorProbe.ref)

      // Send InitiateTarry message to the initiator (process1)
      process1 ! InitiateTarry

      // Verify that the terminator receives the TerminateTarry message
      terminatorProbe.expectMsg(TerminateTarry)
    }

    "perform token traversal and terminate for a ring topology" in {
      val processRecord = new ProcessRecord

      // Create TarryProcess actors based on the topology
      val process1 = system.actorOf(Props(new TarryProcess(1, List(2, 4), initiator = true, processRecord)), "process1_test3")
      val process2 = system.actorOf(Props(new TarryProcess(2, List(1, 3), initiator = false, processRecord)), "process2_test3")
      val process3 = system.actorOf(Props(new TarryProcess(3, List(2, 4), initiator = false, processRecord)), "process3_test3")
      val process4 = system.actorOf(Props(new TarryProcess(4, List(1, 3), initiator = false, processRecord)), "process4_test3")

      // Set up the ProcessRecord
      processRecord.map ++= Map(1 -> process1, 2 -> process2, 3 -> process3, 4 -> process4)

      // Create a TestProbe for the terminator
      val terminatorProbe = TestProbe()
      processRecord.map += (-1 -> terminatorProbe.ref)

      // Send InitiateTarry message to the initiator (process1)
      process1 ! InitiateTarry

      // Verify that the terminator receives the TerminateTarry message
      terminatorProbe.expectMsg(TerminateTarry)
    }
  }
}