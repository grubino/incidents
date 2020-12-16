organization in ThisBuild := "com.luzene"

name := "incident-log"

scalaVersion := "2.13.0"
lazy val akkaHttpVersion = "10.2.1"
lazy val akkaVersion = "2.6.10"
lazy val akkaManagementVersion = "1.0.9"
lazy val akkaCassandraVersion  = "0.102"
lazy val akkaProjectionVersion = "1.0.0"
lazy val alpakkaVersion = "2.0.2"
lazy val chMetricsVersion = "3.0.2"

resolvers += "Maven Repo" at  "https://repo1.maven.org/maven2/"

enablePlugins(JavaServerAppPackaging, DockerPlugin)

// make version compatible with docker for publishing
ThisBuild / dynverSeparator := "-"

scalacOptions := Seq("-feature", "-unchecked", "-deprecation", "-encoding", "utf8")
classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.AllLibraryJars
fork in run := true
Compile / run / fork := true

mainClass in (Compile, run) := Some("com.luzene.Incidents")

dockerExposedPorts := Seq(8080, 8558, 25520)
dockerUpdateLatest := true
dockerUsername := sys.props.get("docker.username")
dockerRepository := sys.props.get("docker.registry")
dockerAlias := DockerAlias(None, None, "bibilthaysose/incidents", Some("5"))
dockerBaseImage := "adoptopenjdk:11-jre-hotspot"

libraryDependencies ++= {
  Seq(
    "com.typesafe.akka" %% "akka-persistence-typed" % akkaVersion,
    "com.typesafe.akka" %% "akka-persistence-cassandra" % akkaCassandraVersion,
    "com.lightbend.akka" %% "akka-stream-alpakka-cassandra" % alpakkaVersion,
    "com.lightbend.akka" %% "akka-projection-cassandra" % akkaProjectionVersion,
    "com.lightbend.akka" %% "akka-projection-core" % akkaProjectionVersion,
    "com.lightbend.akka" %% "akka-projection-eventsourced" % akkaProjectionVersion,
    "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion,
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-cluster-typed" % akkaVersion,
    "com.typesafe.akka" %% "akka-cluster-sharding-typed" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream-typed" % akkaVersion,
    "com.typesafe.akka" %% "akka-discovery" % akkaVersion,
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % akkaManagementVersion,
    "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % akkaManagementVersion,
    "com.lightbend.akka.management" %% "akka-management-cluster-http" % akkaManagementVersion,
    "com.codahale.metrics" % "metrics-core" % chMetricsVersion,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
    "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
    "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test)
}
