addSbtPlugin("ch.epfl.scala"           % "sbt-scalafix"        % "0.10.1")
dependencyOverrides += "ch.epfl.scala" % "scalafix-interfaces" % "0.10.1"
addSbtPlugin("com.github.sbt"          % "sbt-ci-release"      % "1.5.10")
addSbtPlugin("org.scalameta"           % "sbt-scalafmt"        % "2.4.4")
addSbtPlugin("org.scoverage"           % "sbt-scoverage"       % "2.0.0-RC2")
addSbtPlugin("org.scoverage"           % "sbt-coveralls"       % "1.3.1")
addSbtPlugin("ch.epfl.scala"           % "sbt-bloop"           % "1.4.11")
addSbtPlugin("com.eed3si9n"            % "sbt-buildinfo"       % "0.10.0")
addSbtPlugin("org.scalameta"           % "sbt-mdoc"            % "2.2.24")
