import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import main.processes.TreeProcess
import main.utility.{Wave, Info, Decide, TreeTerminate, ProcessRecord, TopologyReader}

class TreeTest extends TestKit(ActorSystem("TreeTest"))
  with ImplicitSender
  with AnyWordSpecLike
  with Matchers
  with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "TreeProcess" should {
    "propagate Wave messages and make decisions based on the network topology" in {
      val processRecord = new ProcessRecord

      // Create TreeProcess actors based on the topology
      val process0 = system.actorOf(Props(new TreeProcess(0, List(1, 2), processRecord)), "process0")
      val process1 = system.actorOf(Props(new TreeProcess(1, List(0, 5), processRecord)), "process1")
      val process2 = system.actorOf(Props(new TreeProcess(2, List(0, 10), processRecord)), "process2")
      val process3 = system.actorOf(Props(new TreeProcess(3, List(7, 8, 13), processRecord)), "process3")
      val process4 = system.actorOf(Props(new TreeProcess(4, List(8, 10, 11), processRecord)), "process4")
      val process5 = system.actorOf(Props(new TreeProcess(5, List(1), processRecord)), "process5")
      val process7 = system.actorOf(Props(new TreeProcess(7, List(3, 9), processRecord)), "process7")
      val process8 = system.actorOf(Props(new TreeProcess(8, List(3, 4, 12), processRecord)), "process8")
      val process9 = system.actorOf(Props(new TreeProcess(9, List(7), processRecord)), "process9")
      val process10 = system.actorOf(Props(new TreeProcess(10, List(2, 4), processRecord)), "process10")
      val process11 = system.actorOf(Props(new TreeProcess(11, List(4), processRecord)), "process11")
      val process12 = system.actorOf(Props(new TreeProcess(12, List(8), processRecord)), "process12")
      val process13 = system.actorOf(Props(new TreeProcess(13, List(3), processRecord)), "process13")

      // Set up the ProcessRecord
      processRecord.map ++= Map(
        0 -> process0, 1 -> process1, 2 -> process2, 3 -> process3, 4 -> process4,
        5 -> process5, 7 -> process7, 8 -> process8, 9 -> process9, 10 -> process10,
        11 -> process11, 12 -> process12, 13 -> process13
      )

      // Create a TestProbe for the terminator
      val terminatorProbe = TestProbe()
      processRecord.map += (-1 -> terminatorProbe.ref)

      // Send initial Wave messages to leaf nodes
      process5 ! Wave
      process9 ! Wave
      process11 ! Wave
      process12 ! Wave
      process13 ! Wave

      // Verify the expected behavior
      terminatorProbe.expectMsg(TreeTerminate)
    }

    "detect a cycle in the topology" in {
      val processConfig = Map(
        "0" -> List("1", "2"),
        "1" -> List("0", "3"),
        "2" -> List("0", "3"),
        "3" -> List("1", "2")
      )

      TopologyReader.hasCycle(processConfig) shouldBe true
    }

    "not detect a cycle in a valid topology" in {
      val processConfig = Map(
        "0" -> List("1", "2"),
        "1" -> List("0", "5"),
        "2" -> List("0", "10"),
        "3" -> List("7", "8", "13"),
        "4" -> List("8", "10", "11"),
        "5" -> List("1"),
        "7" -> List("3", "9"),
        "8" -> List("3", "4", "12"),
        "9" -> List("7"),
        "10" -> List("2", "4"),
        "11" -> List("4"),
        "12" -> List("8"),
        "13" -> List("3")
      )

      TopologyReader.hasCycle(processConfig) shouldBe false
    }
  }
}
