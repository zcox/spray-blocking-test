package blocking

import spray.routing.{SimpleRoutingApp, Route}
import akka.actor.ActorSystem
import scala.concurrent._
import java.util.concurrent.Executors

object Main extends App with SimpleRoutingApp {
  implicit val system = ActorSystem("my-system")
  import system.dispatcher

  def block(delay: Long) = {
    println(s"Blocking for $delay secs...")
    Thread.sleep(delay * 1000) //simulates long-running database query, HTTP request, filesystem access, etc
    println("Done blocking")
    "Done blocking"
  }

  /** Simple route to test responsiveness of service. */
  val pingPong = path("ping")(complete("pong"))

  /** Blocks right on the calling thread. */
  val blockThisThread = path("blockThisThread" / IntNumber)(delay => complete(block(delay)))

  /** Blocks off in a Future, not on calling thread. But note that this Future uses the ActorSystem's default message dispatcher, which the 
    * spray-routing service actor is also using, so it is possible to block all of that dispatcher's threads and prevent spray-routing from 
    * processing new http request messages. */
  val blockInFuture = path("blockInFuture" / IntNumber)(delay => complete(future(block(delay))))

  val context = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(5))
  def poolFuture[T](body: => T): Future[T] = future(body)(context)
  /** Blocks off in a Future that uses its own execution context, not the ActorSystem's default dispatcher. Thus you can hit this to your heart's 
    * content and it will not prevent the spray-routing service actor from handling incoming http request messages. */
  val blockInPoolFuture = path("blockInPoolFuture" / IntNumber)(delay => complete(poolFuture(block(delay))))

  // def detach(route: Route) = detachTo(singleRequestServiceActor)(route)
  /** Spawns a new Actor on each request and blocks on that actor. Same note about message dispatcher applies as for blockInFuture. */
  // val blockInDetach = path("blockInDetach" / IntNumber)(delay => detach(complete(block(delay))))

  

  startServer(interface = "localhost", port = 8080) {
    get(pingPong ~ blockThisThread ~ blockInFuture ~ blockInPoolFuture /*~ blockInDetach*/)
  }
}
