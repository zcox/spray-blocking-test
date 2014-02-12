resolvers += "spray repo" at "http://repo.spray.io"

libraryDependencies ++= {
  val spray = "1.2.0"
  val akka = "2.2.3"
  Seq(
    "com.typesafe.akka" %% "akka-actor" % akka,
    "io.spray" % "spray-can" % spray,
    "io.spray" % "spray-routing" % spray
  )
}

seq(Revolver.settings: _*)
