import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import main.processes.FranklinProcess
import main.utility._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class franklinTest extends TestKit(ActorSystem("BrachaTouegProcessSpec"))
  with AnyWordSpecLike
  with Matchers
  with BeforeAndAfterAll
  with ImplicitSender {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "FranklinProcess" should {

    "should receive Initiate from Orchestrator " in {
      // Create a ProcessRecord instance
      val processRecord = new ProcessRecord


      val process2 = TestProbe()
      val process3 = TestProbe()



      // Register actors in the ProcessRecord

      processRecord.map += (2 -> process2.ref)
      processRecord.map += (3 -> process3.ref)

      //creating orchestrator
      val processes = processRecord.map.keys.filter(_ >= 0).toList
      val orchestrator = system.actorOf(Props(new FranklinOrchestrator(processes, system, processRecord)))

      // Send Initiate message to Orchestrator
      orchestrator ! Initiate

      // Expect Initiate messages sent to Franklin Processes
      process2.expectMsg(Initiate)
      process3.expectMsg(Initiate)
    }


    "LeftNeighbour and RightNeighbour should receive LeftIdRequest and RightIdRequest" in {
      // Create a ProcessRecord instance
      val processRecord = new ProcessRecord
      val left = TestProbe()
      val right = TestProbe()
      val Process = system.actorOf(Props(new FranklinProcess(0, 2, 3, system, processRecord)))

      // Register actors in the ProcessRecord

      processRecord.map += (2 -> left.ref)
      processRecord.map += (3 -> right.ref)


      // Send Initiate message to Franklin Process
      Process ! Initiate

      // Expect Id Request messages sent to Franklin Left and Right neighbour Processes
      left.expectMsg(LeftIdRequest)
      right.expectMsg(RightIdRequest)
    }


  }

}
