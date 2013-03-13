import sbt._
import sbt.Keys._

object BuildSettings {
  val buildSettings = Defaults.defaultSettings ++ Seq(
      organization := "com.kn0de",
      version := "0.1-SNAPSHOT",
      scalaVersion := "2.10.0"
      // add other settings here
    )
}

object RittenhouseBuild extends Build {

  import BuildSettings._

  lazy val rittenhouse = Project(
    id = "rittenhouse",
    base = file("."),
    settings = buildSettings
  ) aggregate(macros, types)


  lazy val macros: Project = Project(
    "macros",
    file("macros"),
    settings = buildSettings ++ Seq(
      libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-compiler" % _))
  )

  lazy val types: Project = Project(
    "types",
    file("types"),
    settings = buildSettings ++ Seq(
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-actor" % "2.1.1",
        "net.debasishg" % "redisclient_2.10" % "2.9",
        "org.scalatest" % "scalatest_2.10" % "2.0.M5b" % "test"
      ),
      initialCommands in console := """
        import com.redis._
        import serialization.Parse.Implicits._
        import rittenhouse.collections._
        import scala.util._
        import scala.concurrent.ExecutionContext.Implicits.global
        import scala.concurrent.Future
        import akka.pattern._
        import scala.concurrent.duration._
        import akka.actor.ActorSystem
        val system = ActorSystem("mySystem")
        implicit val redisClient = new RedisClient
        val testList = new RedisList[Int]("test-redis-list")
      """
    )      
  ) dependsOn(macros)

}
