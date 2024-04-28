import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import main.algorithms.LaiYangAlgorithm
import main.processes.LaiYangProcess
import main.utility.{InitiateSnapshotWithMessageCount, ProcessRecord, SendMessage}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.collection.mutable

class laiYangTest extends TestKit(ActorSystem("SnapshotTest"))
  with AnyWordSpecLike
  with Matchers
  with BeforeAndAfterAll
  with ImplicitSender{

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "LaiYangProcess" should {

    "send out Control Message to neighbour when Snapshot is initiated" in {
      val processRecord = new ProcessRecord

      val process0 = system.actorOf(Props(new LaiYangProcess(0, List(1, 2), false)), "process0_test1")
      val process1 = TestProbe()
      val process2 = TestProbe()

      processRecord.map ++= Map(
        0 -> process0,
        1 -> process1.ref,
        2 -> process2.ref
      )

      LaiYangAlgorithm.init(processRecord)

//      process0 ! InitiateSnapshotActors

      LaiYangAlgorithm.MessageCountStore.preSnapshotMessagesSent ++= Map(
        process0 -> mutable.Map(process1.ref -> 0, process2.ref -> 0),
        process1.ref -> mutable.Map(process0 -> 0, process2.ref -> 0),
        process2.ref -> mutable.Map(process0 -> 0, process1.ref -> 0)
      )

      LaiYangAlgorithm.MessageCountStore.preSnapshotMessagesReceived ++= Map(
        process0 -> mutable.Map(process1.ref -> 0, process2.ref -> 0),
        process1.ref -> mutable.Map(process0 -> 0, process2.ref -> 0),
        process2.ref -> mutable.Map(process0 -> 0, process1.ref -> 0)
      )

      process0 ! InitiateSnapshotWithMessageCount(-1, true)

      process1.expectMsg(InitiateSnapshotWithMessageCount(0, false))
      process2.expectMsg(InitiateSnapshotWithMessageCount(0, false))
    }

    "initiate snapshot process if it receives a message with the tag true" in {
      val processRecord = new ProcessRecord

      val process0 = system.actorOf(Props(new LaiYangProcess(0, List(1), true)), "process0_test2")
      val process1 = system.actorOf(Props(new LaiYangProcess(0, List(2), false)), "process1_test2")
      val process2 = TestProbe()

      processRecord.map ++= Map(
        0 -> process0,
        1 -> process1,
        2 -> process2.ref
      )
      LaiYangAlgorithm.init(processRecord)

      LaiYangAlgorithm.MessageCountStore.preSnapshotMessagesSent ++= Map(
        process0 -> mutable.Map(process1 -> 0),
        process1 -> mutable.Map(process0 -> 0, process2.ref -> 0),
        process2.ref -> mutable.Map(process1 -> 0)
      )

      LaiYangAlgorithm.MessageCountStore.preSnapshotMessagesReceived ++= Map(
        process0 -> mutable.Map(process1 -> 0),
        process1 -> mutable.Map(process0 -> 0, process2.ref -> 0),
        process2.ref -> mutable.Map(process1 -> 0)
      )

//      process0 ! InitiateSnapshotActors
//      process1 ! InitiateSnapshotActors

      process0 ! SendMessage("increment")

      process2.expectMsg(InitiateSnapshotWithMessageCount(0, start = false))
    }
  }

}
