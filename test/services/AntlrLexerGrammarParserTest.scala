package services

import org.scalatestplus.play.PlaySpec

import scalaz.{-\/, \/-}

class AntlrLexerGrammarParserTest extends PlaySpec {
  val lexerGrammarParser = new AntlrLexerGrammarParser(useCache = false)

  "Lexical grammar parser" should {

    "parse valid lexer grammar" in {
      val lg = lexerGrammarParser.parse("lexer grammar l1; A: 'a';")

      lg mustBe a [\/-[_]]
      lg.map(g => {
        g.rules.length mustBe 1
        g.grammar mustBe (null)
        g.lexerGrammar must not be (null)
      })

      lexerGrammarParser.listener.errors mustBe empty
    }

    "return failure for invalid grammar" in {
      val lg = lexerGrammarParser.parse("lexer grammar l1; A: 'a'")

      lg mustBe a [-\/[_]]
      lexerGrammarParser.listener.errors must contain (ParseMessage("lexer","error(50): :1:24: syntax error: '<EOF>' came as a complete surprise to me while looking for lexer rule element","error",24,1))
    }

    "return failure for empty string" in {
      val lg = lexerGrammarParser.parse("")

      lg mustBe a [-\/[_]]
      lexerGrammarParser.listener.errors must contain (ParseMessage("lexer", "Empty grammar is not allowed", "error", 0, 0))
    }

  }
}
