import sbt.Keys.licenses

lazy val commonSettings = Seq(
  organization := "org.universalledger",

  version := "0.1.0-SNAPSHOT",

  scalaVersion := "2.12.18"

)

lazy val root = Project(id="universal_ledger", base = file("."))

lazy val va_pipeline = (project in file("02-foundation/va_pipeline"))
  .settings(
    commonSettings,
    // other settings
  )
