package services

import org.scalatest._
import play.Environment

import scalaz.\/-

class AntlrTextParserTest extends fixture.FlatSpec with MustMatchers {

  case class FixtureParam(grammar: ParseGrammarSuccess)

  def withFixture(test: OneArgTest) = {
    val parser = new AntlrGrammarParser(Environment.simple())
    parser.parseGrammar("grammar test; a: A+; A: 'a';", None).map(g =>
      withFixture(test.toNoArgTest(FixtureParam(g)))
    ).getOrElse(Failed("Grammar cannot be parsed"))
  }

  "AntlrText parser" should
    "parse text with valid grammar" in { g =>
      val parser = new AntlrTextParser(g.grammar)
      val result = parser.parse("a", "")

      result mustBe a [\/-[_]]
      result.map(r => {
        r.tree.rule mustBe "a"
        r.tree.children mustBe empty
      })

    }

}
