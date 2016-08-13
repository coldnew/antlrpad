package services

import models.{Failure, Success}
import org.antlr.v4.tool._
import utils.Cached._
import utils.InMemoryCache

import scalaz.\/
import scalaz.Scalaz._

case class ParseGrammarSuccess(grammar: Grammar, lexerGrammar: LexerGrammar, rules: Seq[String], warnings: Seq[ParseMessage]) extends Success
case class ParseGrammarFailure(errors: Seq[ParseMessage]) extends Failure

class AntlrGrammarParser {

  implicit lazy val inMemoryCache = new InMemoryCache[Int, ParseGrammarFailure \/ ParseGrammarSuccess]()

  def parseGrammar(src: String): ParseGrammarFailure \/ ParseGrammarSuccess = {
    cache by src.hashCode value {
      val tool = new org.antlr.v4.Tool()

      val errorListener = new InternalErrorListener(tool.errMgr)
      tool.removeListeners()
      tool.addListener(errorListener)

      val grammarRootAst = tool.parseGrammarFromString(src)
      val grammar = tool.createGrammar(grammarRootAst)
      tool.process(grammar, false)

      if (errorListener.errors.nonEmpty)
        ParseGrammarFailure(errorListener.errors).left
      else
        ParseGrammarSuccess(grammar, grammar.getImplicitLexer, grammar.getRuleNames, errorListener.warnings).right
    }
  }
}