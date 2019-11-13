package blinky.run

import play.api.libs.json.{Json, Reads}

case class Mutant(id: Int, diff: List[String], original: String, mutated: String)

object Mutant {
  implicit val mutationReads: Reads[Mutant] = Json.reads[Mutant]
}
