package mutators

import mutators.Mutator._
import scalafix.v1._

import scala.meta._

trait MutatorGroup {

  def name: String

  def getSubMutators: List[Mutator]

  def findMutators(str: String): Map[String, Mutator] =
    getSubMutators.map(subMutator => (s"$name.${subMutator.name}", subMutator)).toMap

}

trait Mutator {

  def name: String

  def getMutator(implicit doc: SemanticDocument): PF

  override def toString: String = name

}

object Mutator {

  abstract class SimpleMutator(override val name: String) extends Mutator

  type PF = PartialFunction[Term, (Iterable[Term], Boolean)]

  val allGroups: List[MutatorGroup] =
    List(
      ArithmeticOperators,
      ConditionalExpressions,
      LiteralStrings,
      ScalaOptions
    )

  val all: Map[String, Mutator] =
    Map(
      LiteralBooleans.name -> LiteralBooleans
    ) ++
      allGroups.flatMap(
        group => group.getSubMutators.map(mutator => (s"${group.name}.${mutator.name}", mutator))
      )

  def findMutators(str: String): List[Mutator] = {
    all.collect {
      case (name, mutation) if name == str                => mutation
      case (name, mutation) if name.startsWith(str + ".") => mutation
    }.toList
  }

  case object LiteralBooleans extends SimpleMutator("LiteralBooleans") {
    override def getMutator(implicit doc: SemanticDocument): PF = {
      case Lit.Boolean(value) =>
        default(Lit.Boolean(!value))
    }
  }

  object ArithmeticOperators extends MutatorGroup {

    override val name: String = "ArithmeticOperators"

    override val getSubMutators: List[Mutator] =
      List(IntPlusToMinus, IntMinusToPlus, IntMulToDiv, IntDivToMul)

    object IntPlusToMinus extends SimpleMutator("IntPlusToMinus") {
      override def getMutator(implicit doc: SemanticDocument): PF = {
        case plus @ Term.ApplyInfix(left, Term.Name("+"), targs, right)
            if SymbolMatcher.exact("scala/Int#`+`(+4).").matches(plus.symbol) =>
          default(Term.ApplyInfix(left, Term.Name("-"), targs, right))
      }
    }

    object IntMinusToPlus extends SimpleMutator("IntMinusToPlus") {
      override def getMutator(implicit doc: SemanticDocument): PF = {
        case minus @ Term.ApplyInfix(left, Term.Name("-"), targs, right)
            if SymbolMatcher.exact("scala/Int#`-`(+3).").matches(minus.symbol) =>
          default(Term.ApplyInfix(left, Term.Name("+"), targs, right))
      }
    }

    object IntMulToDiv extends SimpleMutator("IntMulToDiv") {
      override def getMutator(implicit doc: SemanticDocument): PF = {
        case mul @ Term.ApplyInfix(left, Term.Name("*"), targs, right)
            if SymbolMatcher.exact("scala/Int#`*`(+3).").matches(mul.symbol) =>
          default(Term.ApplyInfix(left, Term.Name("/"), targs, right))
      }
    }

    object IntDivToMul extends SimpleMutator("IntDivToMul") {
      override def getMutator(implicit doc: SemanticDocument): PF = {
        case div @ Term.ApplyInfix(left, Term.Name("/"), targs, right)
            if SymbolMatcher.exact("scala/Int#`/`(+3).").matches(div.symbol) =>
          default(Term.ApplyInfix(left, Term.Name("*"), targs, right))
      }
    }

  }

  object ConditionalExpressions extends MutatorGroup {

    override val name: String = "ConditionalExpressions"

    override val getSubMutators: List[Mutator] =
      List(AndToOr, OrToAnd, RemoveUnaryNot)

    object AndToOr extends SimpleMutator("AndToOr") {
      override def getMutator(implicit doc: SemanticDocument): PF = {
        case and @ Term.ApplyInfix(left, Term.Name("&&"), targs, right)
            if SymbolMatcher.exact("scala/Boolean#`&&`().").matches(and.symbol) =>
          default(Term.ApplyInfix(left, Term.Name("||"), targs, right))
      }
    }

    object OrToAnd extends SimpleMutator("OrToAnd") {
      override def getMutator(implicit doc: SemanticDocument): PF = {
        case or @ Term.ApplyInfix(left, Term.Name("||"), targs, right)
            if SymbolMatcher.exact("scala/Boolean#`||`().").matches(or.symbol) =>
          default(Term.ApplyInfix(left, Term.Name("&&"), targs, right))
      }
    }

    object RemoveUnaryNot extends SimpleMutator("RemoveUnaryNot") {
      override def getMutator(implicit doc: SemanticDocument): PF = {
        case boolNeg @ Term.ApplyUnary(Term.Name("!"), arg)
            if SymbolMatcher.exact("scala/Boolean#`unary_!`().").matches(boolNeg.symbol) =>
          default(arg)
      }
    }

  }

  object LiteralStrings extends MutatorGroup {

    override val name: String = "LiteralStrings"

    override val getSubMutators: List[Mutator] =
      List(EmptyToMutated, NonEmptyToMutated, ConcatToMutated)

    object EmptyToMutated extends SimpleMutator("EmptyToMutated") {
      override def getMutator(implicit doc: SemanticDocument): PF = {
        case Lit.String(value) if value.isEmpty =>
          default(Lit.String("mutated!"))
      }
    }

    object NonEmptyToMutated extends SimpleMutator("NonEmptyToMutated") {
      override def getMutator(implicit doc: SemanticDocument): PF = {
        case Lit.String(value) if value.nonEmpty =>
          default(Lit.String(""), Lit.String("mutated!"))
      }
    }

    object ConcatToMutated extends SimpleMutator("ConcatToMutated") {
      override def getMutator(implicit doc: SemanticDocument): PF = {
        case concat @ Term.ApplyInfix(_, Term.Name("+"), _, _)
            if SymbolMatcher.exact("java/lang/String#`+`().").matches(concat.symbol) =>
          //List(left, right, Lit.String("mutated!"), Lit.String(""))
          fullReplace(Lit.String("mutated!"), Lit.String(""))
      }
    }

  }

  object ScalaOptions extends MutatorGroup {

    override val name: String = "ScalaOptions"

    override val getSubMutators: List[Mutator] =
      List(
        GetOrElse,
        Exists,
        Forall,
        IsEmpty,
        NonEmpty,
        Fold,
        OrElse,
        OrNull,
        Filter,
        FilterNot,
        Contains
      )

    object GetOrElse extends SimpleMutator("GetOrElse") {
      override def getMutator(implicit doc: SemanticDocument): PF = {
        case getOrElse @ Term.Apply(Term.Select(_, Term.Name("getOrElse")), List(arg))
            if SymbolMatcher.exact("scala/Option#getOrElse().").matches(getOrElse.symbol) =>
          default(arg)
      }
    }

    object Exists extends SimpleMutator("Exists") {
      override def getMutator(implicit doc: SemanticDocument): PF = {
        case exists @ Term.Apply(Term.Select(termName, Term.Name("exists")), args)
            if SymbolMatcher.exact("scala/Option#exists().").matches(exists.symbol) =>
          default(Term.Apply(Term.Select(termName, Term.Name("forall")), args))
      }
    }

    object Forall extends SimpleMutator("Forall") {
      override def getMutator(implicit doc: SemanticDocument): PF = {
        case forall @ Term.Apply(Term.Select(termName, Term.Name("forall")), args)
            if SymbolMatcher.exact("scala/Option#forall().").matches(forall.symbol) =>
          default(Term.Apply(Term.Select(termName, Term.Name("exists")), args))
      }
    }

    object IsEmpty extends SimpleMutator("IsEmpty") {
      override def getMutator(implicit doc: SemanticDocument): PF = {
        case isEmpty @ Term.Select(termName, Term.Name("isEmpty"))
            if SymbolMatcher.exact("scala/Option#isEmpty().").matches(isEmpty.symbol) =>
          default(Term.Select(termName, Term.Name("nonEmpty")))
      }
    }

    object NonEmpty extends SimpleMutator("NonEmpty") {
      override def getMutator(implicit doc: SemanticDocument): PF = {
        case nonEmpty @ Term.Select(termName, Term.Name("nonEmpty" | "isDefined"))
            if SymbolMatcher.exact("scala/Option#nonEmpty().").matches(nonEmpty.symbol) ||
              SymbolMatcher.exact("scala/Option#isDefined().").matches(nonEmpty.symbol) =>
          default(Term.Select(termName, Term.Name("isEmpty")))
      }
    }

    object Fold extends SimpleMutator("Fold") {
      override def getMutator(implicit doc: SemanticDocument): PF = {
        case fold @ Term.Apply(Term.Apply(Term.Select(_, Term.Name("fold")), List(argDefault)), _)
            if SymbolMatcher.exact("scala/Option#fold().").matches(fold.symbol) =>
          default(argDefault)
      }
    }

    object OrElse extends SimpleMutator("OrElse") {
      override def getMutator(implicit doc: SemanticDocument): PF = {
        case orElse @ Term.Apply(Term.Select(termName, Term.Name("orElse")), List(arg))
            if SymbolMatcher.exact("scala/Option#orElse().").matches(orElse.symbol) =>
          default(termName, arg)
      }
    }

    object OrNull extends SimpleMutator("OrNull") {
      override def getMutator(implicit doc: SemanticDocument): PF = {
        case orNull @ Term.Select(_, Term.Name("orNull"))
            if SymbolMatcher.exact("scala/Option#orNull().").matches(orNull.symbol) =>
          default(Lit.Null())
      }
    }

    object Filter extends SimpleMutator("Filter") {
      override def getMutator(implicit doc: SemanticDocument): PF = {
        case filter @ Term.Apply(Term.Select(termName, Term.Name("filter")), args)
            if SymbolMatcher.exact("scala/Option#filter().").matches(filter.symbol) =>
          default(termName, Term.Apply(Term.Select(termName, Term.Name("filterNot")), args))
      }
    }

    object FilterNot extends SimpleMutator("FilterNot") {
      override def getMutator(implicit doc: SemanticDocument): PF = {
        case filterNot @ Term.Apply(Term.Select(termName, Term.Name("filterNot")), args)
            if SymbolMatcher.exact("scala/Option#filterNot().").matches(filterNot.symbol) =>
          default(termName, Term.Apply(Term.Select(termName, Term.Name("filter")), args))
      }
    }

    object Contains extends SimpleMutator("Contains") {
      override def getMutator(implicit doc: SemanticDocument): PF = {
        case contains @ Term.Apply(Term.Select(_, Term.Name("contains")), _)
            if SymbolMatcher.exact("scala/Option#contains().").matches(contains.symbol) =>
          default(Lit.Boolean(true), Lit.Boolean(false))
      }
    }

  }

  def default(terms: Term*): (List[Term], Boolean) = (terms.toList, false)

  def fullReplace(terms: Term*): (List[Term], Boolean) = (terms.toList, true)

  def empty: (List[Term], Boolean) = (List.empty, false)
}
