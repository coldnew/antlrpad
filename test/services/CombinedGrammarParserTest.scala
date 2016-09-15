package services

import org.scalatestplus.play.PlaySpec

import scalaz.{-\/, \/-}

class CombinedGrammarParserTest extends PlaySpec {
  val combinedParser = new AntlrGrammarParser(false, None)

  "Combined grammar parser" should {

    "parse valid combined grammar" in {
      val g = combinedParser.parse("grammar c1; a: A; A: 'a';")

      g mustBe a [\/-[_]]
      g.map(g => {
        g.rules.length mustBe 1
      })

      combinedParser.listener.errors mustBe empty
    }

    "return failure for invalid grammar" in {
      val g = combinedParser.parse("lexer grammar l1; A: 'a'")

      g mustBe a [-\/[_]]
      combinedParser.listener.errors must contain (ParseMessage("parser","error(50): :1:24: syntax error: '<EOF>' came as a complete surprise to me while looking for lexer rule element","error",24,1))
    }

    "return failure for empty string" in {
      val g = combinedParser.parse("")

      g mustBe a [-\/[_]]
      combinedParser.listener.errors must contain (ParseMessage("parser", "Empty grammar is not allowed", "error", 0, 0))
    }

  }
}