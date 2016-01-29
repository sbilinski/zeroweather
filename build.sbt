import com.typesafe.sbt.SbtScalariform._

//
// Commons
//
lazy val commonSettings = Seq(
  organization  := "com.mintbeans",
  version       := "0.1",
  startYear     := Some(2016),
  scalaVersion  := "2.11.7",
  updateOptions := updateOptions.value.withCachedResolution(true),
  scalacOptions ++= Seq("-feature", "-unchecked", "-deprecation", "-encoding", "utf8"),
  resolvers     ++= Seq(
    "Sonatype Snapshots"  at "https://oss.sonatype.org/content/repositories/snapshots/",
    "Sonatype Releases"   at "http://oss.sonatype.org/content/repositories/releases",
    "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
    //msgpack4s
    "velvia maven"        at "http://dl.bintray.com/velvia/maven"
  ),
  libraryDependencies ++= {
    val configVersion       = "1.3.0"
    val akkaVersion         = "2.4.1"
    val msgpack4sVersion    = "0.5.1"
    val jeromqVersion       = "0.3.5"
    val scalaLoggingVersion = "3.1.0"
    val logbackVersion      = "1.1.3"
    val scalaMockVersion    = "3.2.1"
    Seq(
      "com.typesafe"               %   "config"                      % configVersion,
      "com.typesafe.akka"          %%  "akka-actor"                  % akkaVersion,
      "com.typesafe.akka"          %%  "akka-slf4j"                  % akkaVersion,
      "com.typesafe.akka"          %%  "akka-testkit"                % akkaVersion % "test",
      "org.velvia"                 %%  "msgpack4s"                   % msgpack4sVersion,
      "org.zeromq"                 %   "jeromq"                      % jeromqVersion,
      "com.typesafe.scala-logging" %%  "scala-logging"               % scalaLoggingVersion,
      "ch.qos.logback"             %   "logback-classic"             % logbackVersion,
      "org.scalamock"              %%  "scalamock-scalatest-support" % scalaMockVersion % "test",
      //Required for IntelliJ ScalaTest integration
      "org.scala-lang.modules"     %%  "scala-xml"                   % "1.0.1" % "test"
    )
  }
)

// 
// Projects
//
lazy val communication = project.in(file("communication"))
                            .settings(commonSettings: _*)
                            .settings(SbtScalariform.scalariformSettings: _*)
                            .settings(
                              libraryDependencies ++= {
                                Seq(
                                  "org.mockito"       %  "mockito-all"   % "1.10.19" % "test"
                                )
                              }
                            )

lazy val message   = project.in(file("message"))
                            .settings(commonSettings: _*)
                            .settings(SbtScalariform.scalariformSettings: _*)

lazy val proxy     = project.in(file("proxy"))
                            .settings(commonSettings: _*)
                            .settings(SbtScalariform.scalariformSettings: _*)
                            .settings(Revolver.settings: _*)
                            .settings(
                              mainClass in Revolver.reStart := Some("zeroweather.proxy.Proxy")
                            )
                            .dependsOn(message)
                            .dependsOn(communication)
                            .settings(
                              libraryDependencies ++= {
                                val akkaStreamVersion = "2.0.2"
                                Seq(
                                  "com.typesafe.akka" %% "akka-http-experimental"             % akkaStreamVersion,
                                  "com.typesafe.akka" %% "akka-http-spray-json-experimental"  % akkaStreamVersion,
                                  "com.typesafe.akka" %% "akka-http-testkit-experimental"     % akkaStreamVersion % "test"
                                )
                              }
                            )

lazy val supplier  = project.in(file("supplier"))
                            .settings(commonSettings: _*)
                            .settings(SbtScalariform.scalariformSettings: _*)
                            .settings(Revolver.settings: _*)
                            .settings(
                              mainClass in Revolver.reStart := Some("zeroweather.supplier.Supplier")
                            )
                            .dependsOn(message)
                            .dependsOn(communication)

lazy val root      = project.in(file(".")).aggregate(message, proxy, supplier, communication)
