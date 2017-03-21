name := "genmc"

version := "1.0"

organization := "com.uncarved"

scalaVersion := "2.12.1"

scalacOptions ++= Seq("-unchecked", "-deprecation")
scalacOptions in Test ++= Seq("-Yrangepos")

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
 
libraryDependencies ++= Seq( 
	"org.specs2" %% "specs2-core" % "3.8.9" % "test",
	 "com.typesafe.akka" %% "akka-actor" % "2.4.17"
	)
