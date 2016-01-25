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
    "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
  ),
  libraryDependencies ++= {
    val configVersion     = "1.3.0"
    val scalaMockVersion  = "3.2.1"
    Seq(
      "com.typesafe"              %   "config"                      % configVersion,
      "org.scalamock"             %%  "scalamock-scalatest-support" % scalaMockVersion % "test"
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

lazy val supplier  = project.in(file("supplier"))
                            .settings(commonSettings: _*)
                            .settings(SbtScalariform.scalariformSettings: _*)
                            .settings(Revolver.settings: _*)
                            .settings(
                              mainClass in Revolver.reStart := Some("zeroweather.supplier.Supplier")
                            )
                            .dependsOn(message)

lazy val root      = project.in(file(".")).aggregate(message, proxy, supplier)
