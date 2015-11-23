import sbt.Keys._
import sbt.Project.projectToRef

// a special crossProject for configuring a JS/JVM/shared structure
lazy val shared = (crossProject.crossType(CrossType.Pure) in file("shared"))
  .settings(
    scalaVersion := Settings.versions.scala,
    libraryDependencies ++= Settings.sharedDependencies.value
  )
  // set up settings specific to the JS project
  .jsConfigure(_ enablePlugins ScalaJSPlay)

lazy val sharedJVM = shared.jvm.settings(name := "sharedJVM")

lazy val sharedJS = shared.js.settings(name := "sharedJS")

// use eliding to drop some debug code in the production build
lazy val elideOptions = settingKey[Seq[String]]("Set limit for elidable functions")

// instantiate the JS project for SBT with some additional settings
lazy val client: Project = (project in file("client"))
  .settings(
    name := "client",
    version := Settings.version,
    scalaVersion := Settings.versions.scala,
    scalacOptions ++= Settings.scalacOptions,
    libraryDependencies ++= Settings.scalajsDependencies.value,
    // by default we do development build, no eliding
    elideOptions := Seq(),
    scalacOptions ++= elideOptions.value,
    // RuntimeDOM is needed for tests
    jsDependencies += RuntimeDOM % "test",
    // yes, we want to package JS dependencies
    skip in packageJSDependencies := false,
    // use Scala.js provided launcher code to start the client app
    //    persistLauncher := true,
    //    persistLauncher in Test := false,
    // use uTest framework for tests
    testFrameworks += new TestFramework("utest.runner.Framework")
  )
  .enablePlugins(ScalaJSPlugin, ScalaJSPlay)
  .dependsOn(sharedJS)

lazy val copyClientFile = taskKey[Iterable[sbt.File]]("Copy client file to a more significant name")
copyClientFile := {
  val paths: Map[String, String] =
    Map("client/target/scala-2.11/client-fastopt.js" -> "client/dist/scripts/octopus-on-wire.js",
      "server/target/web/less/main/stylesheets/main.min.css" -> "client/dist/styles/octopus-on-wire.min.css",
      "server/target/web/public/images/next_icon.png" -> "client/dist/images/next_icon.png"
    )
  val toCopy: Map[sbt.File, sbt.File] = (for {
    (src, dest) <- paths
    original = file(src)
    copied = file(dest)
  } yield original -> copied).toMap

  toCopy.foreach(p => IO.copyFile(p._1, p._2))
  toCopy.values
}


// Client projects (just one in this case)
lazy val clients = Seq(client)

// instantiate the JVM project for SBT with some additional settings
lazy val server = (project in file("server"))
  .settings(
    name := "server",
    version := Settings.version,
    scalaVersion := Settings.versions.scala,
    scalacOptions ++= Settings.scalacOptions,
    libraryDependencies ++= Settings.jvmDependencies.value,
    libraryDependencies += ws,
    // connect to the client project
    scalaJSProjects := clients,
    pipelineStages := Seq(scalaJSProd),
//     compress CSS
    LessKeys.compress in Assets := true
  )
  .enablePlugins(PlayScala)
  .disablePlugins(PlayLayoutPlugin) // use the standard directory layout instead of Play's custom
  .aggregate(clients.map(projectToRef): _*)
  .dependsOn(sharedJVM)

lazy val root = (project in file(".")).aggregate(client, server)

// loads the Play server project at sbt startup
onLoad in Global := (Command.process("project server", _: State)) compose (onLoad in Global).value
