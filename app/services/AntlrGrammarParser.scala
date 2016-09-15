package services

import com.google.inject.Inject
import models.{Failure, Success}
import org.antlr.v4.Tool
import org.antlr.v4.tool._
import utils.Cached._
import utils.InMemoryCache

import scala.util.Try
import scalaz.\/
import scalaz.Scalaz._

case class ParseGrammarSuccess(grammar: Grammar, lexerGrammar: LexerGrammar, rules: Seq[String], warnings: Seq[ParseMessage]) extends Success
case class ParseGrammarFailure(errors: Seq[ParseMessage]) extends Failure

abstract class AntlrBaseGrammarParser (useCache: Boolean) {

  implicit lazy val inMemoryCache = new InMemoryCache[Int, Option[Grammar]]()

  protected val tool = new org.antlr.v4.Tool()
  val listener: GrammarParserErrorListener

  def preProcessGrammar(grammar: Grammar): Grammar

  def parse(grammarSource: String): ParseGrammarFailure \/ ParseGrammarSuccess = {
    val grammar = parseGrammar(grammarSource)

    if (listener.errors.isEmpty && grammar.isDefined) {
      val g = grammar.get
      ParseGrammarSuccess(g, g.implicitLexer, g.getRuleNames, listener.warnings).right
    }
    else {
      ParseGrammarFailure(listener.errors).left
    }
  }

  private def parseGrammar(src: String): Option[Grammar] = {
    if (src == null || src.isEmpty) {
      listener.error("Empty grammar is not allowed")
      None
    }
    else {
      cache(useCache) by src.hashCode value {
        tool.removeListeners()
        tool.addListener(listener)

        val grammarRootAst = tool.parseGrammarFromString(src)
        val grammar = preProcessGrammar(tool.createGrammar(grammarRootAst))

        Try {
          tool.process(grammar, false)
          grammar
        }.toOption
      }
    }
  }
}

class AntlrLexerGrammarParser(useCache: Boolean) extends AntlrBaseGrammarParser(useCache) {

  override val listener: GrammarParserErrorListener = new GrammarParserErrorListener(tool.errMgr, ParseMessage.SourceLexer)
  override def preProcessGrammar(grammar: Grammar): Grammar = grammar

}

class AntlrGrammarParser(useCache: Boolean, lexer: Option[LexerGrammar]) extends AntlrBaseGrammarParser(useCache) {
  override val listener: GrammarParserErrorListener = new GrammarParserErrorListener(tool.errMgr, ParseMessage.SourceParser)
  override def preProcessGrammar(grammar: Grammar): Grammar = {
    lexer.map(l => {
      grammar.importVocab(l)
      grammar
    }).getOrElse(grammar)
  }
}