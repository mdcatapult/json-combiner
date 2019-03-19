import sbt.Credentials
import sbt.Keys.{credentials, version}

lazy val root = (project in file(".")).settings(
  name              := "json-combiner",
  version           := "1.0",
  scalaVersion      := "2.12.8",
  scalacOptions     += "-Ypartial-unification",
  resolvers         += "Artifactory" at "http://nexus.mdcatapult.io/repository/maven-public/",
  credentials       += {
    val artifactoryPassword = sys.env.get("ARTIFACTORY_PASSWORD")
    if ( artifactoryPassword.nonEmpty ) {
      Credentials("Sonatype Nexus Repository Manager", "nexus.mdcatapult.io","gitlab",artifactoryPassword.get)
    } else {
      Credentials(Path.userHome / ".sbt" / ".credentials")
    }
  },
  libraryDependencies ++= {
    lazy val configVersion = "1.3.3"
    lazy val akkaVersion = "2.5.18"
    lazy val catsVersion = "1.5.0-RC1"
    Seq(
      "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
      "com.typesafe" % "config" % configVersion,
      "org.typelevel" %% "cats-macros" % catsVersion,
      "org.typelevel" %% "cats-kernel" % catsVersion,
      "com.github.scopt" %% "scopt" % "4.0.0-RC2",
      "com.typesafe.play" %% "play-json" % "2.7.1",
      "org.micchon" %% "play-json-xml" % "0.4.2"
    )
  }

)


