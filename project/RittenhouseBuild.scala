import sbt._
import sbt.Keys._

object RittenhouseBuild extends Build {

  lazy val rittenhouse = Project(
    id = "rittenhouse",
    base = file("."),
    settings = Project.defaultSettings ++ Seq(
      name := "rittenhouse",
      organization := "com.kn0de",
      version := "0.1-SNAPSHOT",
      scalaVersion := "2.10.0"
      // add other settings here
    )
  )
}
