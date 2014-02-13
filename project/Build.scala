import sbt._
import sbt.Keys._

object EverythingBuild extends samskivert.MavenBuild {

  override val globalSettings = Seq(
    crossPaths      := false,
    javacOptions    ++= Seq("-Xlint", "-Xlint:-serial", "-source", "1.6", "-target", "1.6"),
    scalacOptions   ++= Seq("-unchecked", "-deprecation", "-feature",
                            "-language:implicitConversions"),
    fork in Compile := true,
    libraryDependencies ++= Seq(
      "com.novocode" % "junit-interface" % "0.7" % "test->default" // make JUnit tests work
    ),
    resolvers += "Local Maven Repository" at Path.userHome.asURL + "/.m2/repository"
  )
}
