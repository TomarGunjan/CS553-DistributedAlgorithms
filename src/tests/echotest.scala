import akka.actor.ActorSystem
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

class EchoProcessSpec extends WordSpec with Matchers with BeforeAndAfterAll {
  implicit val system: ActorSystem = ActorSystem("EchoAlgorithmSystem")

  override def afterAll(): Unit = {
    system.terminate()
    super.afterAll()
  }

  "An EchoProcess actor" should {
    "increment count when receiving EchoWave messages" in {
      val echoProcess = system.actorOf(Props(new EchoProcess("1", List.empty, initiator = false)))

      // Simulate sending EchoWave message to the actor
      echoProcess ! EchoWave()

      // Wait for a short duration to allow the actor to process the message
      Thread.sleep(500)

      // Check if the count has been incremented
      // You would need to modify this assertion based on your actual implementation
      // This is just a hypothetical example
      echoProcess ! "getCount" // Hypothetical message to get the current count
      expectMsg(1) // Assuming the count should be 1 after receiving one EchoWave message
    }
  }
}
