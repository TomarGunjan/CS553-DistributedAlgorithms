import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import main.algorithms.LaiYangAlgorithm
import main.processes.LaiYangProcess
import main.utility.{InitiateSnapshotActors, InitiateSnapshotWithMessageCount, PerformActionWithTagPayload, ProcessRecord, SendMessage}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

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

      val process0 = system.actorOf(Props(new LaiYangProcess(0, List(1, 2), false)), "process0")
      val process1 = TestProbe()
      val process2 = TestProbe()

      processRecord.map ++= Map(
        0 -> process0,
        1 -> process1.ref,
        2 -> process2.ref
      )

      LaiYangAlgorithm.init(processRecord)

      process0 ! InitiateSnapshotActors

      process0 ! InitiateSnapshotWithMessageCount(-1, true)

      process1.expectMsg(InitiateSnapshotWithMessageCount(0, false))
      process2.expectMsg(InitiateSnapshotWithMessageCount(0, false))
    }

    "initiate snapshot process if it receives a message with the tag true" in {
      val processRecord = new ProcessRecord

      val process0 = system.actorOf(Props(new LaiYangProcess(0, List(1), true)), "process0")
      val process1 = system.actorOf(Props(new LaiYangProcess(0, List(2), false)), "process1")
      val process2 = TestProbe()
//      val process2 = TestProbe()
//      val process1 = system.actorOf(Props(new LaiYangProcess(0, List(2), false)), "process1")
//      val process2 = TestProbe()

      processRecord.map ++= Map(
        0 -> process0,
        1 -> process1,
        2 -> process2.ref
      )
//
      LaiYangAlgorithm.init(processRecord)
//
      process0 ! InitiateSnapshotActors
      process1 ! InitiateSnapshotActors

//      process0 ! InitiateSnapshotWithMessageCount(-1, true)

      process0 ! SendMessage("increment")

//      process1.expectMsg(PerformActionWithTagPayload("increment", snapshotTaken = true))
      process2.expectMsg(InitiateSnapshotWithMessageCount(0, start = false))

//      process1 ! InitiateSnapshotActors
//
//      process0 ! InitiateSnapshotWithMessageCount(-1, true)
//      process1 ! SendMessage("increment")
//
////      process1.ref ! PerformActionWithTagPayload("increment", true)
////      process0 ! InitiateSnapshotWithMessageCount(-1, true)
//
////      process0 ! SendMessage("increment")
//
//      process1.ref ! PerformActionWithTagPayload("increment", true)
//
//      process1.expectMsg(InitiateSnapshotWithMessageCount(-1, true))

//      val processRecord = new ProcessRecord
//
//      val probe = TestProbe()
//      val probe1 = TestProbe()
//      val myActorRef = system.actorOf(Props(new LaiYangProcess(0, List(1), false)), "process0")
//
//      processRecord.map ++= Map(
//        0 -> myActorRef,
//        1 -> probe.ref
//      )
//
//      LaiYangAlgorithm.init(processRecord)
//
//      myActorRef ! InitiateSnapshotActors
//
//      myActorRef ! PerformActionWithTagPayload("increment", true)
//
//      probe.expectMsg(InitiateSnapshotWithMessageCount(-1, true))

//      val process0 = TestProbe()
//
//      process0.ref ! InitiateSnapshotActors
//
//      process0.ref ! PerformActionWithTagPayload("increment", true)
//
//      process0.expectMsg(InitiateSnapshotWithMessageCount(-1, true))

//      process0.
    }
  }

}
