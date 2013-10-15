Demonstrates that blocking directly in routes can cause subsequent HTTP requests to timeout, along with several methods of working around this. spray-routing's SimpleRoutingApp uses a single service actor instance to route every single incoming HTTP request message on the default Akka dispatcher, and this fact can cause problems if you call long-running synchronous blocking functions in your routes. 

To run, simply do `re-start` in sbt.

If you `curl http://localhost:8080/blockThisThread/10` and then try to `curl http://localhost:8080/ping` those ping requests will timeout, because the single service actor's runRoute function is blocked by Thread.sleep, those requests pile up in its mailbox, and then something lower-level in Spray times out.

The `/blockInFuture/10` and `/blockInDetach/10` routes will (mostly) avoid this behavior because the Thread.sleep calls now block some other thread and allow the service actor's runRoute function to continue processing incoming HTTP request messages. Note though, that these other Futures/Actors are still running on the default Akka dispatcher, as is the service actor, and are blocking its threads. So eventually if enough requests for those routes come in concurrently, you will run out of threads and the service actor will be unable to continue processing incoming request messages. You can force this behavior by running many `curl http://localhost:8080/blockInFuture/10 &` calls concurrently. The number of calls depends on the number of threads in the default dispatcher's pool.

To ensure that the service actor can continue handling incoming http request messages, the `/blockInPoolFuture/10` route does the blocking off in a separate Future under a separate execution context, not in the default Akka dispatcher. So you can run as many `curl http://localhost:8080/blockInPoolFuture/10 &` requests as you want, and `/ping` will continue to respond immediately.

So some best practices probably are:
  * don't call long-running functions directly in routes
  * do call long-running functions separately in a Future or detachTo or some other Actor
  * think about using some other ExecutionContext/Dispatcher for those long-running functions
