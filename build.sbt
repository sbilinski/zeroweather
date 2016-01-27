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
    val msgpack4sVersion    = "0.5.1"
    val jeromqVersion       = "0.3.5"
    val scalaLoggingVersion = "3.1.0"
    val slf4jVersion        = "1.7.14"
    val scalaMockVersion    = "3.2.1"
    Seq(
      "com.typesafe"               %   "config"                      % configVersion,
      "org.velvia"                 %%  "msgpack4s"                   % msgpack4sVersion,
      "org.zeromq"                 %   "jeromq"                      % jeromqVersion,
      "com.typesafe.scala-logging" %%  "scala-logging"               % scalaLoggingVersion,
      "org.slf4j"                  %   "slf4j-simple"                % slf4jVersion,
      "org.scalamock"              %%  "scalamock-scalatest-support" % scalaMockVersion % "test",
      //Required for IntelliJ ScalaTest integration
      "org.scala-lang.modules"     %%  "scala-xml"                   % "1.0.1" % "test"
    )
  }
)

// 
// Projects
// 
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
                            .settings(
                              libraryDependencies ++= {
                                Seq(
                                  "com.typesafe.akka" %% "akka-actor" % "2.4.1"
                                )
                              }
                            )


lazy val root      = project.in(file(".")).aggregate(message, proxy, supplier)
