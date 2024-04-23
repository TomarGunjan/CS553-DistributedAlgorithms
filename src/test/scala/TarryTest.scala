import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import main.processes.TarryProcess
import main.utility.{InitiateTarry, TarryProbe, TerminateTarry, ProcessRecord}

class TarryProcessSpec extends TestKit(ActorSystem("TarryProcessSpec"))
  with ImplicitSender
  with AnyWordSpecLike
  with Matchers
  with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "TarryProcess" should {
    "perform token traversal and terminate when the initiator receives tokens through all channels" in {
      val processRecord = new ProcessRecord

      // Create TarryProcess actors based on the topology
      val process1 = system.actorOf(Props(new TarryProcess(1, List(2, 3, 4), initiator = true, processRecord)), "process1")
      val process2 = system.actorOf(Props(new TarryProcess(2, List(1, 5, 6), initiator = false, processRecord)), "process2")
      val process3 = system.actorOf(Props(new TarryProcess(3, List(1, 5), initiator = false, processRecord)), "process3")
      val process4 = system.actorOf(Props(new TarryProcess(4, List(1, 6), initiator = false, processRecord)), "process4")
      val process5 = system.actorOf(Props(new TarryProcess(5, List(2, 3), initiator = false, processRecord)), "process5")
      val process6 = system.actorOf(Props(new TarryProcess(6, List(2, 4), initiator = false, processRecord)), "process6")

      // Set up the ProcessRecord
      processRecord.map ++= Map(
        1 -> process1, 2 -> process2, 3 -> process3,
        4 -> process4, 5 -> process5, 6 -> process6
      )

      // Create a TestProbe for the terminator
      val terminatorProbe = TestProbe()
      processRecord.map += (-1 -> terminatorProbe.ref)

      // Send InitiateTarry message to the initiator (process1)
      process1 ! InitiateTarry

      // Verify the expected behavior
      terminatorProbe.expectMsg(TerminateTarry)
    }
  }
}