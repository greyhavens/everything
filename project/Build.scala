import java.io.File

import sbt._
import sbt.Keys._

import net.thunderklaus.GwtPlugin._
import net.virtualvoid.sbt.graph.Plugin.graphSettings
import samskivert.POMUtil

object EverythingBuild extends Build {
  val asyncGen = TaskKey[Unit]("async-gen", "Generates GWT service Async classes")
  private def asyncGenTask =
    (streams, sourceDirectory, classDirectory in Compile, dependencyClasspath in Compile) map {
      (s, sourceDir, classes, depCP) => {
        val cp = (classes +: depCP.map(_.data)) map(_.toURI.toURL)
        val loader = java.net.URLClassLoader.newInstance(cp.toArray)
        val genner = new com.samskivert.asyncgen.AsyncGenerator(loader, null) {
          override def fail (message :String, cause :Throwable) =
            new RuntimeException(message, cause)
        }
        val sources = (sourceDir ** "*Service.java").get
        s.log.debug("Generating async interfaces for: " + sources.mkString(", "))
        sources foreach { genner.processInterface(_) }
      }
    }

  val i18nSync = TaskKey[Unit]("i18n-sync", "Generates i18n Messages interfaces from properties")
  private def i18nSyncTask =
    (streams, javaSource in Compile) map {
      (s, sourceDir) => {
        val props = (sourceDir ** "*Messages.properties").get
        s.log.debug("Generating i18n interfaces for: " + props.mkString(", "))
        props foreach { f => com.threerings.gwt.tools.I18nSync.processFile(sourceDir, f) }
      }
    }

  val pom = pomutil.POM.fromFile(new File("pom.xml")).get
  val pomSettings = POMUtil.pomToSettings(pom, false)
  val baseSettings = Defaults.defaultSettings ++ pomSettings ++ gwtSettings ++ graphSettings

  val everything = Project("everything", file("."), settings = baseSettings ++ seq(
    crossPaths    := false,
    scalaVersion  := "2.10.0",
    javacOptions  ++= Seq("-Xlint", "-Xlint:-serial", "-Xlint:-path",
                          "-source", "1.6", "-target", "1.6"),
    scalacOptions ++= Seq("-unchecked", "-deprecation"),
    // no scala-library dependency
    autoScalaLibrary := false,

    // pass some options to the server when we run it
    fork in Compile := true,
    javaOptions in Compile ++= Seq(
      "-ea",
      "-Djava.util.logging.config.file=target/webapp/WEB-INF/classes/logging.properties",
      "-Dorg.eclipse.jetty.util.log.class=org.eclipse.jetty.util.log.JavaUtilLog",
      "-Dapproot=target/webapp",
      "-Denv.file=etc/everything.conf"),

    // GWT plugin bits
    gwtVersion := pom.getAttr("gwt.version").get,
    gwtDevModeArgs := Seq("-noserver"),
    javaOptions in Gwt ++= Seq("-mx512M"), // give GWT some memory juices
    asyncGen <<= asyncGenTask, // task for regenerating GWT async ifaces

    // write classes directly into the webapp directory for easier Jetty testing
    classDirectory in Compile <<= (target) { _ / "webapp" / "WEB-INF" / "classes" },

    // get our snapshot depends from Maven, sigh
    resolvers ++= Seq(
      "Local Maven Repository" at Path.userHome.asURL + ".m2/repository"
    ),

    // depends for running jetty during testing
    libraryDependencies ++= Seq(
      "org.mortbay.jetty" % "jetty" % "6.1.22" % "container",
      "com.novocode" % "junit-interface" % "0.7" % "test->default"
    )
  ))

  // gwtTemporaryPath <<= (target) { (target) => target / "webapp" },
}
