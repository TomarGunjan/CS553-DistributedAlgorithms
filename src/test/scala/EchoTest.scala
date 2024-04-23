import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import main.processes.EchoProcess
import main.utility.{EchoWave, EchoTerminate}

class EchoProcessSpec extends TestKit(ActorSystem("EchoProcessSpec"))
  with ImplicitSender
  with AnyWordSpecLike
  with Matchers
  with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "EchoProcess" should {
    "propagate Wave messages to neighbors and send Wave back to parent" in {
      // Create EchoProcess actors based on the topology
      val process1 = system.actorOf(Props(new EchoProcess("1", List("2", "4", "5"), initiator = true)), "1")
      val process2 = system.actorOf(Props(new EchoProcess("2", List("1", "3", "5"), initiator = false)), "2")
      val process3 = system.actorOf(Props(new EchoProcess("3", List("2"), initiator = false)), "3")
      val process4 = system.actorOf(Props(new EchoProcess("4", List("1", "5"), initiator = false)), "4")
      val process5 = system.actorOf(Props(new EchoProcess("5", List("1", "2", "4"), initiator = false)), "5")

      // Process 1 is the initiator, so it should send Wave messages to its neighbors
      val process2Probe = TestProbe()
      val process4Probe = TestProbe()
      val process5Probe = TestProbe()
      process2Probe.send(process2, EchoWave())
      process4Probe.send(process4, EchoWave())
      process5Probe.send(process5, EchoWave())

      // Process 2 should set Process 1 as its parent and send Wave messages to its other neighbors
      val process3Probe = TestProbe()
      val process5Probe2 = TestProbe()
      process3Probe.send(process3, EchoWave())
      process5Probe2.send(process5, EchoWave())

      // Process 3 should set Process 2 as its parent and send Wave back to Process 2
      val process2Probe2 = TestProbe()
      process2Probe2.send(process2, EchoWave())

      // Process 4 should set Process 1 as its parent and send Wave messages to its other neighbors
      val process5Probe3 = TestProbe()
      process5Probe3.send(process5, EchoWave())

      // Process 5 should set Process 1 as its parent and send Wave messages to its other neighbors
      val process2Probe3 = TestProbe()
      val process4Probe2 = TestProbe()
      process2Probe3.send(process2, EchoWave())
      process4Probe2.send(process4, EchoWave())
    }
  }
}