package services

import org.antlr.v4.tool.{Grammar, LexerGrammar}
import utils.Cached._

import scalaz._
import Scalaz._

object Defaults {
  import utils.InMemoryCache

  implicit lazy val inMemoryCache = new InMemoryCache[Int, \/[ParseGrammarFailure, ParseGrammarSuccess]]()
}

case class ParseGrammarSuccess(grammar: Grammar, lexerGrammar: LexerGrammar, rules: Seq[String])
case class ParseGrammarFailure(errors: Seq[String])

class AntlrGrammarParser {

  import Defaults.inMemoryCache

  def parseGrammar(src: String): \/[ParseGrammarFailure, ParseGrammarSuccess] = {
    cache by src.hashCode value {

      val tool = new org.antlr.v4.Tool()
      val grammarRootAst = tool.parseGrammarFromString(src)
      if (!grammarRootAst.hasErrors) {
        val parsedGrammar = tool.createGrammar(grammarRootAst)
        tool.process(parsedGrammar, false)
        ParseGrammarSuccess(parsedGrammar, parsedGrammar.getImplicitLexer, parsedGrammar.getRuleNames).right
      }
      else ParseGrammarFailure(Seq("Cannot parse grammar")).left

    }
  }
}