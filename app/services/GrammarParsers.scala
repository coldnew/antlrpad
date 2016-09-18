package services

import models.{Failure, Success}
import org.antlr.v4.tool._
import utils.Cached._
import utils.InMemoryCache

import scalaz.Scalaz._
import scalaz.\/

sealed trait ParseGrammarSuccess extends Success
case class EmptyGrammar() extends ParseGrammarSuccess
case class ParsedGrammar(grammar: Grammar, lexerGrammar: LexerGrammar, rules: Seq[String], warnings: Seq[ParseMessage]) extends ParseGrammarSuccess

case class ParseGrammarFailure(errors: Seq[ParseMessage]) extends Failure

abstract class AntlrBaseGrammarParser (useCache: Boolean) {

  implicit lazy val inMemoryCache = new InMemoryCache[Int, ParseGrammarFailure \/ ParseGrammarSuccess]()

  protected val tool = new org.antlr.v4.Tool()

  val listener: GrammarParserErrorListener
  def preProcessGrammar(grammar: Grammar): Grammar
  def getResult(g: Grammar): ParsedGrammar

  def parse(src: String): ParseGrammarFailure \/ ParseGrammarSuccess = {
    if (src == null || src.isEmpty) {
      EmptyGrammar().right
    }
    else {
      cache(useCache) by src.hashCode value {
        tool.removeListeners()
        tool.addListener(listener)

        val grammarRootAst = tool.parseGrammarFromString(src)
        val grammar = preProcessGrammar(tool.createGrammar(grammarRootAst))

        tool.process(grammar, false)

        if (listener.errors.isEmpty)
          getResult(grammar).right
        else
          ParseGrammarFailure(listener.errors).left
      }
    }
  }
}

class AntlrLexerGrammarParser(useCache: Boolean) extends AntlrBaseGrammarParser(useCache) {
  override val listener: GrammarParserErrorListener = new GrammarParserErrorListener(tool.errMgr, ParseMessage.SourceLexer)
  override def preProcessGrammar(grammar: Grammar): Grammar = grammar
  override def getResult(g: Grammar): ParsedGrammar = ParsedGrammar(null, g.asInstanceOf[LexerGrammar], g.getRuleNames, listener.warnings)
}

class AntlrGrammarParser(useCache: Boolean, lexer: ParseGrammarSuccess) extends AntlrBaseGrammarParser(useCache) {
  override val listener: GrammarParserErrorListener = new GrammarParserErrorListener(tool.errMgr, ParseMessage.SourceParser)
  override def preProcessGrammar(grammar: Grammar): Grammar = {
    lexer match {
      case eg: EmptyGrammar => grammar
      case lg: ParsedGrammar => {
        grammar.importVocab(lg.lexerGrammar)
        grammar
      }
    }
  }
  override def getResult(g: Grammar): ParsedGrammar = {
    lexer match {
      case _: EmptyGrammar => ParsedGrammar(g, g.getImplicitLexer, g.getRuleNames, listener.warnings)
      case lg: ParsedGrammar => ParsedGrammar(g, lg.lexerGrammar, g.getRuleNames, listener.warnings ++ lg.warnings)
    }
  }
}