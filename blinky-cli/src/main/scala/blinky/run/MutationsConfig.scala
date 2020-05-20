package blinky.run

import blinky.v0.BlinkyConfig
import metaconfig.generic.Surface
import metaconfig.typesafeconfig._
import metaconfig.{Conf, ConfDecoder, generic}

case class MutationsConfig(
    projectPath: String,
    projectName: String,
    filesToMutate: String,
    filesToExclude: String,
    conf: BlinkyConfig,
    options: OptionsConfig
)

object MutationsConfig {
  val default: MutationsConfig = MutationsConfig(
    projectPath = ".",
    projectName = "",
    filesToMutate = "src/main/scala",
    filesToExclude = "",
    conf = BlinkyConfig.default,
    options = OptionsConfig.default
  )

  implicit val surface: Surface[MutationsConfig] =
    generic.deriveSurface[MutationsConfig]
  implicit val decoder: ConfDecoder[MutationsConfig] =
    generic.deriveDecoder(default).noTypos

  def read(conf: String): MutationsConfig = {
    val original = decoder.read(Conf.parseString(conf)).get
    val projectName = original.projectName
    if (projectName.isEmpty)
      original
    else
      original.copy(
        options = original.options.copy(
          compileCommand = projectName,
          testCommand = projectName
        )
      )
  }
}
