lazy val example =
  project
    .in(file("."))
    .settings(
      scalaVersion := "2.13.11",
      libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.15" % Test
    )
