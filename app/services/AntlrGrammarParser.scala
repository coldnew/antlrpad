package services

import org.antlr.v4.tool.{Grammar, LexerGrammar}
import utils.Cached._

object Defaults {
  import utils.InMemoryCache

  implicit lazy val inMemoryCache = new InMemoryCache[Int, (Option[Grammar], Option[LexerGrammar])]()
}

class AntlrGrammarParser {

  def parseGrammar(src: String): (Option[Grammar], Option[LexerGrammar]) = {
    import Defaults.inMemoryCache

    cache by src.hashCode value {
      val tool = new org.antlr.v4.Tool()
      val grammarRootAst = tool.parseGrammarFromString(src)

      val grammar = Option(tool.createGrammar(grammarRootAst))
      grammar.foreach(g => tool.process(g, false))

      (grammar, grammar.map(g => g.getImplicitLexer))
    }

  }

}