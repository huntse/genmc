name := "genmc"

version := "1.0"

organization := "com.uncarved"

scalaVersion := "2.9.1"

scalacOptions ++= Seq("-unchecked", "-deprecation")

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
 
libraryDependencies ++= Seq( 
	"org.specs2" % "specs2_2.9.0" % "1.3" % "test",
	 "com.typesafe.akka" % "akka-actor" % "2.0"
	)
