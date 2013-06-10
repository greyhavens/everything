// pom-util POM helpers
libraryDependencies += "com.samskivert" % "sbt-pom-util" % "0.4"

// this GWT plugin also pulls in xsbt-web-plugin
addSbtPlugin("net.thunderklaus" % "sbt-gwt-plugin" % "1.1-SNAPSHOT")

resolvers += "thunderklaus repo" at "http://thunderklaus.github.com/maven"

// used to debug dependencies
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.6.0")

// wire up jrebel
addSbtPlugin("io.spray" % "sbt-revolver" % "0.6.2")

// for our asyncgen and i18n sync tasks
libraryDependencies ++= Seq(
  "com.samskivert" % "gwt-asyncgen" % "1.0",
  "com.threerings" % "gwt-utils" % "1.5"
)
