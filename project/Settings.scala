import sbt._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

/**
  * Application settings. Configure the build for your application here.
  * You normally don't have to touch the actual build definition after this.
  */
object Settings {
  /** The name of your application */
  val name = "octopus-on-wire"

  /** The version of your application */
  val version = "1.0.0"

  /** Options for the scala compiler */
  val scalacOptions = Seq(
    "-Xlint",
    "-unchecked",
    "-deprecation",
    "-feature"
  )

  /** Set some basic options when running the project with Revolver */
  val jvmRuntimeOptions = Seq(
    "-Xmx1G"
  )

  /** Declare global dependency versions here to avoid mismatches in multi part dependencies */
  object versions {
    val scala = "2.11.8"
    val scalaDom = "0.9.0"
    val scalaRx = "0.3.1"
    val autowire = "0.2.5"
    val booPickle = "1.1.2"
    val uTest = "0.3.1"

    val bootstrap = "3.3.2"

    val scalatags = "0.5.4"
    val scalatest = "2.2.6"
    val playSlick = "2.0.0"
    val postgres = "9.1-901-1.jdbc4"

    val bower = "4.5.0"
  }

  /**
    * These dependencies are shared between JS and JVM projects
    * the special %%% function selects the correct version for each project
    */
  val sharedDependencies = Def.setting(Seq(
    "com.lihaoyi" %%% "autowire" % versions.autowire,
    "me.chrons" %%% "boopickle" % versions.booPickle
  ))

  /** Dependencies only used by the JVM project */
  val jvmDependencies = Def.setting(Seq(
    "org.webjars.bower" % "font-awesome" % versions.bower % Provided,
    "org.webjars" % "bootstrap" % versions.bootstrap % Provided,
    "org.scalatest" % "scalatest_2.11" % versions.scalatest % "test",
    "postgresql" % "postgresql" % versions.postgres,
    "com.typesafe.play" %% "play-slick" % versions.playSlick,
    "com.typesafe.play" %% "play-slick-evolutions" % versions.playSlick
  ))

  /** Dependencies only used by the JS project (note the use of %%% instead of %%) */
  val scalajsDependencies = Def.setting(Seq(
    "org.scala-js" %%% "scalajs-dom" % versions.scalaDom,
    "com.lihaoyi" %%% "scalarx" % versions.scalaRx,
    "com.lihaoyi" %%% "scalatags" % versions.scalatags,
    "com.lihaoyi" %%% "utest" % versions.uTest
  ))
}
