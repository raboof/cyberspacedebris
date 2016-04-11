name := "cyberspacedebris"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.8"

scalacOptions += "-target:jvm-1.7"

javacOptions ++= Seq("-target", "1.7")

unmanagedResources in Compile += baseDirectory.value / "README.md"

libraryDependencies ++= Seq(
  "com.typesafe.akka"      %% "akka-actor"                        % "2.4.3",
  "com.typesafe.akka"      %% "akka-slf4j"                        % "2.4.3",
  "com.typesafe.akka"      %% "akka-http-core"                    % "2.4.3",
  "com.typesafe.akka"      %% "akka-http-spray-json-experimental" % "2.4.3",
  "ch.qos.logback"          % "logback-classic"                   % "1.1.7",
  "com.sanoma.cda"         %% "maxmind-geoip2-scala"              % "1.5.1"
)

