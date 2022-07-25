package blinky.v0.mutators

import blinky.v0.Mutator.{MutationResult, default}
import blinky.v0.{Mutator, MutatorGroup}
import scalafix.v1.{SemanticDocument, SymbolMatcher, XtensionTreeScalafix}

import scala.meta.Term

object ScalaTry extends MutatorGroup {
  override val groupName: String = "ScalaTry"

  override val getSubMutators: List[Mutator] =
    List(
      GetOrElse,
      OrElse
    )

  object GetOrElse extends SimpleMutator("GetOrElse") {
    override def getMutator(implicit doc: SemanticDocument): MutationResult = {
      case getOrElse @ Term.Apply(Term.Select(termName, Term.Name("getOrElse")), List(arg))
          if SymbolMatcher.exact("scala/util/Try#getOrElse().").matches(getOrElse.symbol) =>
        default(Term.Select(termName, Term.Name("get")), arg)
    }
  }

  object OrElse extends SimpleMutator("OrElse") {
    override def getMutator(implicit doc: SemanticDocument): MutationResult = {
      case orElse @ Term.Apply(Term.Select(termName, Term.Name("orElse")), List(arg))
          if SymbolMatcher.exact("scala/util/Try#orElse().").matches(orElse.symbol) =>
        default(termName, arg)
    }
  }

}