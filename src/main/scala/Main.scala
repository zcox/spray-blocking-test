package banno

import spray.routing.SimpleRoutingApp
import akka.actor.ActorSystem
import scala.concurrent.Future

object Main extends App with SimpleRoutingApp {
  implicit val system = ActorSystem("my-system")
  import system.dispatcher

  startServer(interface = "localhost", port = 8080) {
    get(pingPong ~ blockThisThread ~ blockInFuture ~ blockInDetach)
  }

  def block(delay: Long) = {
    println(s"Blocking for $delay secs...")
    Thread.sleep(delay * 1000)
    println("Done blocking")
    "Done blocking"
  }

  def blockThisThread = path("blockThisThread" / IntNumber)(delay => complete(block(delay)))
  def blockInFuture = path("blockInFuture" / IntNumber)(delay => complete(Future(block(delay))))
  def blockInDetach = path("blockInDetach" / IntNumber)(delay => detachTo(singleRequestServiceActor)(complete(block(delay))))
  def pingPong = path("ping")(complete("pong"))
}
