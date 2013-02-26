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

  libraryDependencies += "net.debasishg" % "redisclient_2.10" % "2.9"

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
      libraryDependencies += "net.debasishg" % "redisclient_2.10" % "2.9"
    )      
  ) dependsOn(macros)

}
