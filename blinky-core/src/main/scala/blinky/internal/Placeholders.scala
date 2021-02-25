package blinky.internal

import blinky.internal.MutatedTerms.{PlaceholderMutatedTerms, StandardMutatedTerms}

import java.util.UUID
import scala.meta.{Name, Term}
import scala.meta.Term.{Apply, ApplyInfix, Placeholder, Select}

object Placeholders {

  def replacePlaceholders(
      original: Term,
      mutatedTerms: MutatedTerms
  ): Option[(Term, MutatedTerms)] =
    (original, mutatedTerms) match {
      case (Placeholder(), _) =>
        None
      case (original, mutatedTerms: StandardMutatedTerms)
          if original.collect { case Placeholder() => }.nonEmpty =>
        println(
          s"""${"-" * 80}
             |$original
             |${original.structure}
             |${"#" * 0}
             |${mutatedTerms.mutated.zipWithIndex.map { case (m, i) => s"$i: $m" }.mkString("\n")}
             |""".stripMargin
        )
        val isBasicPlaceholderCase =
          original match {
            case ApplyInfix(Placeholder(), _, _, _) =>
              true
            case ApplyInfix(Select(Apply(_, placeholdersList), _), _, _, _)
                if containsPlaceholders(placeholdersList) =>
              true
            case Apply(Select(Apply(Select(Placeholder(), _), _), _), _) =>
              true
            case _ =>
              false
          }

        if (isBasicPlaceholderCase) {
          println(mutatedTerms.mutated.map(_.structure))

          val newVar = Term.Name(s"blinky_${UUID.randomUUID().toString}")
          println(newVar)

          val placeholderFunction = generatePlaceholderFunction(newVar)
          val mutatedReplaced =
            mutatedTerms.mutated.map(term => (term, replaceFirstPlaceholder(term, newVar)))

          val originalReplaced =
            replaceFirstPlaceholder(original, newVar)

          Some(
            (
              original,
              PlaceholderMutatedTerms(
                originalReplaced,
                placeholderFunction,
                mutatedReplaced,
                Seq(newVar.value),
                mutatedTerms.needsParens
              )
            )
          )
        } else {
          None
        }
      case other =>
        Some(other)
    }

  private def containsPlaceholders(list: List[Term]): Boolean =
    list.exists {
      case Placeholder() => true
      case _             => false
    }

  private def generatePlaceholderFunction(newVar: Term.Name): Term => Term =
    (body: Term) =>
      Term.Function(
        List(Term.Param(List.empty, newVar, None, None)),
        body
      )

  private def replaceFirstPlaceholder(initialTerm: Term, newVar: Term.Name): Term = {
    def replace(term: Term): (Term, Boolean) =
//      println(("replace", term.structure))
      term match {
        case Placeholder() =>
          (newVar, true)
        case _: Name =>
          (term, false)
        case ApplyInfix(lhs, op, targs, args) =>
          val (lhsReplaced, varReplaced) = replace(lhs)
//          println("&" * 20)
//          println(lhs.structure)
//          println((lhsReplaced.structure, varReplaced))
//          println("&" * 20)
          if (varReplaced)
            (ApplyInfix(lhsReplaced, op, targs, args), false)
          else {
            val (argsUpdated, varReplaced) =
              args.foldRight((List.empty[Term], false)) {
                case (arg, (argsUpdated, false)) =>
                  replace(arg) match {
                    case (argUpdated, varReplaced) =>
                      (argUpdated :: argsUpdated, varReplaced)
                  }
                case (arg, (argsUpdated, true)) =>
                  (arg :: argsUpdated, true)
              }
//            println("$" * 20)
//            println(args)
//            println(argsUpdated)
//            println("$" * 20)
            (
              ApplyInfix(
                lhs,
                op,
                targs,
                argsUpdated
              ),
              varReplaced
            )
          }
        case Select(term, name) =>
          val (replacedTerm, varReplaced) = replace(term)
          (Select(replacedTerm, name), varReplaced)
        case Apply(applyTerm, terms) =>
          val (replacedTerm, varReplaced) = replace(applyTerm)
          if (varReplaced)
            (Apply(replacedTerm, terms), varReplaced)
          else {
            val (termsUpdated, varReplaced) =
              terms.foldRight((List.empty[Term], false)) {
                case (term, (termsUpdated, false)) =>
                  replace(term) match {
                    case (termUpdated, varReplaced) =>
                      (termUpdated :: termsUpdated, varReplaced)
                  }
                case (term, (termsUpdated, true)) =>
                  (term :: termsUpdated, true)
              }
            (Apply(applyTerm, termsUpdated), varReplaced)
          }
        case other =>
//          println("+" * 20)
//          println(other.structure)
//          println("+" * 20)
          (other, false)
      }

//    Term.Function(
//      List(Term.Param(List.empty, newVar, None, None)),
//    )
    replace(initialTerm)._1
  }

}
