import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import main.algorithms.ChandyLamportAlgorithm
import main.processes.ChandyLamportProcess
import main.utility.{InitiateSnapshot, PerformAction, ProcessRecord, SendMessage}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class chandyLamportTest extends TestKit(ActorSystem("SnapshotTest"))
  with AnyWordSpecLike
  with Matchers
  with BeforeAndAfterAll
  with ImplicitSender {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "ChandyLamportProcess" should {

    "should send out Marker to neighbour when Snapshot is initiated" in {

      val processRecord = new ProcessRecord

      val process0 = system.actorOf(Props(new ChandyLamportProcess(0, List(1, 2), false)), "process0_test1")
      val process1 = TestProbe()
      val process2 = TestProbe()

      processRecord.map ++= Map(
        0 -> process0,
        1 -> process1.ref,
        2 -> process2.ref
      )

      ChandyLamportAlgorithm.init(processRecord)

      process0 ! InitiateSnapshot(true)

      process1.expectMsg(InitiateSnapshot(false))
      process2.expectMsg(InitiateSnapshot(false))

    }

    "should send out a message to one of its neighbors when SendMessage action is performed" in {

      val processRecord = new ProcessRecord

      val process0 = system.actorOf(Props(new ChandyLamportProcess(0, List(1), false)), "process0_test2")
      val process1 = TestProbe()

      processRecord.map ++= Map(
        0 -> process0,
        1 -> process1.ref,
      )

      ChandyLamportAlgorithm.init(processRecord)

      process0 ! SendMessage("increment")

      process1.expectMsgAnyOf(PerformAction("increment"))
      expectNoMessage()

    }

  }

}
