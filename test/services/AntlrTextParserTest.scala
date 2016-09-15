package services

import org.scalatest._
import scalaz.\/-

class AntlrTextParserTest extends fixture.FlatSpec with MustMatchers {

  case class FixtureParam(grammar: ParseGrammarSuccess)

  def withFixture(test: OneArgTest) = {
    val parser = new AntlrGrammarParser(false, None)
    parser.parse("grammar test; a: A+; A: 'a';").map(g =>
      withFixture(test.toNoArgTest(FixtureParam(g)))
    ).getOrElse(Failed("Grammar cannot be parsed"))
  }

  "AntlrText parser" should
    "parse valid expression" in { g =>
      val parser = new AntlrTextParser(g.grammar)
      val result = parser.parse("a", "")

      result mustBe a [\/-[_]]
      result.map(r => {
        r.tree.rule mustBe "a"
        r.tree.children mustBe empty
      })

    }

  it should "return errors for invalid expression" in { g =>
    val parser = new AntlrTextParser(g.grammar)
    val result = parser.parse("b", "")

    result mustBe a [\/-[_]]
    result.map(r => {
      r.messages.length mustBe (1)
      r.messages must contain (ParseMessage("text","missing 'a' at '<EOF>'","error",1,1))
    })
  }

}
