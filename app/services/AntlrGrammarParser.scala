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

class AntlrGrammarParser @Inject() (env: play.Environment) {

  implicit lazy val inMemoryCache = new InMemoryCache[Int, ParseGrammarFailure \/ ParseGrammarSuccess]()

  def parseGrammar(src: String, lexer: Option[String]): ParseGrammarFailure \/ ParseGrammarSuccess = {
    cache(env.isProd) by src.hashCode value {
      val tool = new org.antlr.v4.Tool()

      val errorListener = new GrammarParserErrorListener(tool.errMgr)
      tool.removeListeners()
      tool.addListener(errorListener)

      val grammar = parseGrammar(src, tool)
      val lexerGrammar = lexer
        .map(_.trim)
        .filter(!_.isEmpty)
        .flatMap(lexerSrc => Try {
          val lg = parseGrammar(lexerSrc, tool).asInstanceOf[LexerGrammar]
          tool.process(lg, false)
          grammar.importVocab(lg)

          lg
        }.toOption)

      tool.process(grammar, false)

      if (errorListener.errors.nonEmpty)
        ParseGrammarFailure(errorListener.all).left
      else
        ParseGrammarSuccess(grammar, lexerGrammar.getOrElse(grammar.getImplicitLexer), grammar.getRuleNames, errorListener.warnings).right
    }
  }

  private def parseGrammar(src: String, tool: Tool): Grammar = {
    val grammarRootAst = tool.parseGrammarFromString(src)
    val grammar = tool.createGrammar(grammarRootAst)
    grammar
  }
}