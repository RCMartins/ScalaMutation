import $file.utils, utils._
import ammonite.ops._

@main
def main(): Unit = {
  val basePath = pwd
  val versionNumber = publishLocalBlinky()

  val defaultDirectory = basePath / "examples" / "default"
  val exampleDirectories = ls(basePath / "examples")

  val examples: Seq[(Path, CommandResult)] =
    exampleDirectories.filterNot(_.baseName == "default").map {
      examplePath =>
        println("\n")
        val msg = s"Testing $examplePath:"
        println("-" * msg.length)
        println(msg)
        println("-" * msg.length)

        preProcessDirectory(defaultDirectory, examplePath)

        val confPath = examplePath / ".blinky.conf"
        val result = %%(
          "coursier",
          "launch",
          s"com.github.rcmartins:blinky-cli_2.12:$versionNumber",
          "--main",
          "blinky.cli.Cli",
          "--",
          confPath,
          "--verbose",
          "true"
        )(examplePath)
        println(result.out.string)
        (examplePath, result)
    }

  val brokenExamples = examples.filter(_._2.exitCode != 0)

  if (brokenExamples.nonEmpty) {
    println("There were broken tests:")
    println(brokenExamples.map { case (path, _) => s"$path" }.mkString("\n"))
    System.exit(1)
  }
}

private def preProcessDirectory(defaultDirectory: Path, testDirectory: Path): Unit = {
  %("bash", "-c", s"""cp -nr $defaultDirectory/* $testDirectory""")(pwd)

  val startupScriptName = "startup.sh"
  if (exists(testDirectory / startupScriptName)) {
    %("chmod", "+x", startupScriptName)(testDirectory)
    %(s"./$startupScriptName")(testDirectory)
  }
}
