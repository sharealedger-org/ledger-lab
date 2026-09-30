import sbtassembly.AssemblyPlugin.autoImport._

lazy val root = (project in file("."))
  .settings(
    name := "va_pipeline",
    scalaVersion := "2.12.18",
    Compile / unmanagedSourceDirectories += baseDirectory.value / "../../04-engines/calculation_engines/src/main/scala",
    Compile / packageBin / mainClass := Some("org.universalledger.foundation.va.ledger.LedgerApp"),

    exportJars := true,

    // Exclude the Spark-dependent source files from compilation.
    // Steps 1, 10, 11 (DPBSpark, initFiles, poToPayMatch, poVendorID) require Spark.
    // Steps 2-9 compile cleanly without Spark on Java 21.
    Compile / unmanagedSources / excludeFilter := new FileFilter {
      def accept(f: java.io.File): Boolean =
        f.getCanonicalPath.contains("foundation" + java.io.File.separator + "va" + java.io.File.separator + "spark")
    },


    // Spark 2.4.0 does not support Java 21. Spark is only required for Step 11 (DPBSpark.scala).
    // Steps 2, 3, 4, 5, 6, 7, 8, 9 are pure Scala and have no Spark dependency.
    // To restore Spark for Step 11: uncomment the lines below and recompile.
    //
    // libraryDependencies ++= Seq(
    //   "org.apache.spark" %% "spark-core" % "3.5.3",
    //   "org.apache.spark" %% "spark-sql"  % "3.5.3"
    // ),

    libraryDependencies += "org.postgresql" % "postgresql" % "42.7.4",
    libraryDependencies += "org.apache.commons" % "commons-csv" % "1.12.0",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.19" % Test,

    // sbt-assembly merge strategy (updated syntax for sbt-assembly 2.x)
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", _*) => MergeStrategy.discard
      case _                        => MergeStrategy.first
    }
  )
