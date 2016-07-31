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

  def parseGrammar(src: String): \/[ParseGrammarFailure, ParseGrammarSuccess] = {
    val tool = new org.antlr.v4.Tool()
    val grammarRootAst = tool.parseGrammarFromString(src)

    val parsedGrammar = Option(tool.createGrammar(grammarRootAst))
    parsedGrammar.foreach(tool.process(_, false))
    val lexerGrammar = parsedGrammar.flatMap(g => Option(g.getImplicitLexer)) // parsedGrammar.map(_.getImplicitLexer) doesn't work here at returns Some(null)
    (parsedGrammar, lexerGrammar) match {
      case (Some(g), Some(lg)) => ParseGrammarSuccess(g, lg, g.getRuleNames).right
      case _ => ParseGrammarFailure(Seq("Cannot parse grammar")).left
    }
  }
}