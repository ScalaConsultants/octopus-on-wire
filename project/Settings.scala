import sbt._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

/**
  * Application settings. Configure the build for your application here.
  * You normally don't have to touch the actual build definition after this.
  */
object Settings {
  /** The name of your application */
  val name = "scalajs-spa"

  /** The version of your application */
  val version = "1.0.2"

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
    val scala = "2.11.7"
    val scalaDom = "0.8.1"
    val scalaRx = "0.2.8"
    val autowire = "0.2.5"
    val booPickle = "1.1.0"
    val uTest = "0.3.1"

    val bootstrap = "3.3.2"

    val playScripts = "0.3.0"
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
    "com.vmunier" %% "play-scalajs-scripts" % versions.playScripts,
    "org.webjars.bower" % "font-awesome" % "4.5.0" % Provided,
    "org.webjars" % "bootstrap" % versions.bootstrap % Provided,
    "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"
  ))

  /** Dependencies only used by the JS project (note the use of %%% instead of %%) */
  val scalajsDependencies = Def.setting(Seq(
    "org.scala-js" %%% "scalajs-dom" % versions.scalaDom,
    "com.lihaoyi" %%% "scalarx" % versions.scalaRx,
    "com.lihaoyi" %%% "scalatags" % "0.4.6",
    "com.lihaoyi" %%% "utest" % versions.uTest
  ))
}
