package services

import org.antlr.v4.tool.{Grammar, LexerGrammar}
import org.scalatest.{Failed, MustMatchers, Outcome, fixture}

import scalaz.{-\/, \/-}

class ParserAndLexerGrammarParserTest extends fixture.FlatSpec with MustMatchers {
  case class FixtureParam(parser: AntlrGrammarParser, lexerGrammar: LexerGrammar)

  override def withFixture(test: OneArgTest): Outcome = {
    val lexerGrammarParser = new AntlrLexerGrammarParser(useCache = false)
    val lexer = lexerGrammarParser.parse("lexer grammar l1; A: 'a';")

    lexer.map(l => {
      val parser = new AntlrGrammarParser(false, Some(l.lexerGrammar))
      val param = FixtureParam(parser, l.lexerGrammar)
      test(param)
    }).getOrElse(Failed("Cannot parse lexer grammar"))
  }

  "Parser grammar with lexer provided" should
    "Parse parser grammar" in { p =>
      val lexerGrammar = p.lexerGrammar
      val grammar = p.parser.parse("parser grammar l1; a: A;")

      grammar mustBe a [\/-[_]]
      grammar.map(g => {
        g.grammar must not be (null)
        g.lexerGrammar must not be (null)
        g.lexerGrammar mustBe (lexerGrammar)
      })

      p.parser.listener.errors mustBe empty
    }
}