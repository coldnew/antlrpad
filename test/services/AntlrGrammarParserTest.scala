package services

import org.scalatestplus.play.PlaySpec
import play.Environment

import scalaz.{-\/, \/-}

class AntlrGrammarParserTest extends PlaySpec {
  val parser = new AntlrGrammarParser(Environment.simple())

  "Grammar parser" should {

    "parse combined grammar" in {
      val grammar = parser.parseGrammar("grammar test; a: 'A'+;", None)

      grammar mustBe a [\/-[_]]
    }

    "return failure for invalid grammar" in {
      val grammar = parser.parseGrammar("grammar test; a: 'A'+", None)

      grammar mustBe a [-\/[_]]
    }

    "import lexer vocab when provided" in {
      val grammar = parser.parseGrammar("parser grammar l1; a: A;", Some("lexer grammar l1; A: 'A';"))

      grammar mustBe a [\/-[_]]
      grammar.map { g =>
        val ruleName = g.grammar.getTokenName(1)

        ruleName mustBe "A"
      }
    }

    "return failure for invalid lexer grammar" in {
      val grammar = parser.parseGrammar("parser grammar l1; a: A;", Some("lexer grammar l1; A: 'A'"))

      grammar mustBe a [-\/[_]]
    }

  }
}
