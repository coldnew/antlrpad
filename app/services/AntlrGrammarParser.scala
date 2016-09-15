package services

import com.google.inject.Inject
import models.{Failure, Success}
import org.antlr.v4.Tool
import org.antlr.v4.tool._
import utils.Cached._
import utils.InMemoryCache

import scalaz.\/
import scalaz.Scalaz._

case class ParseGrammarSuccess(grammar: Grammar, lexerGrammar: LexerGrammar, rules: Seq[String], warnings: Seq[ParseMessage]) extends Success
case class ParseGrammarFailure(errors: Seq[ParseMessage]) extends Failure

abstract class AntlrBaseGrammarParser (useCache: Boolean) {

  implicit lazy val inMemoryCache = new InMemoryCache[Int, Option[Grammar]]()

  protected val tool = new org.antlr.v4.Tool()
  val listener: GrammarParserErrorListener

  def parseGrammar(src: String): Option[Grammar] = {
    if (src == null || src.isEmpty) {
      listener.error("Empty grammar is not allowed")
      None
    }
    else {
      cache(useCache) by src.hashCode value {

        tool.removeListeners()
        tool.addListener(listener)

        val grammarRootAst = tool.parseGrammarFromString(src)
        val grammar = tool.createGrammar(grammarRootAst)

        tool.process(grammar, false)

        Option(grammar)
      }
    }
  }
}

class AntlrLexerGrammarParser(useCache: Boolean) extends AntlrBaseGrammarParser(useCache) {

  override val listener: GrammarParserErrorListener = new GrammarParserErrorListener(tool.errMgr, ParseMessage.SourceLexer)

  def parse(grammarSource: String): ParseGrammarFailure \/ ParseGrammarSuccess = {
    val grammar = parseGrammar(grammarSource).map(_.asInstanceOf[LexerGrammar])

    if (listener.errors.isEmpty && grammar.isDefined) {
      val g = grammar.get
      ParseGrammarSuccess(g, g.implicitLexer, g.getRuleNames, listener.warnings).right
    }
    else {
      ParseGrammarFailure(listener.errors).left
    }
  }

}

class AntlrGrammarParser(useCache: Boolean)  {
}