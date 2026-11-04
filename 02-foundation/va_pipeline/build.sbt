import sbtassembly.AssemblyPlugin.autoImport._

lazy val root = (project in file("."))
  .settings(
    name := "va_pipeline",
    scalaVersion := "2.12.20",
    Compile / unmanagedSourceDirectories += baseDirectory.value / "../../04-engines/calculation_engines/src/main/scala",
    Compile / packageBin / mainClass := Some("org.universalledger.foundation.va.ledger.LedgerApp"),

    exportJars := true,

    libraryDependencies += "org.postgresql" % "postgresql" % "42.7.7",
    libraryDependencies += "org.apache.commons" % "commons-csv" % "1.14.0",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.19" % Test,

    // sbt-assembly merge strategy (updated syntax for sbt-assembly 2.x)
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", _*) => MergeStrategy.discard
      case _                        => MergeStrategy.first
    }
  )
