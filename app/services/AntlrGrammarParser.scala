package services

import org.antlr.v4.tool.{ANTLRMessage, ANTLRToolListener, Grammar, LexerGrammar}
import utils.Cached._

object Defaults {
  import utils.InMemoryCache

  implicit lazy val inMemoryCache = new InMemoryCache[Int, Either[ParseGrammarFailure, ParseGrammarSuccess]]()
}

case class ParseGrammarSuccess(grammar: Grammar, lexerGrammar: LexerGrammar, rules: Seq[String])
case class ParseGrammarFailure(errors: Seq[ParseError])
case class ParseError(message: String, col: Int, line: Int)

class AntlrGrammarParser {

  import Defaults.inMemoryCache

  implicit def convertError(antlrError: ANTLRMessage): ParseError = ParseError(antlrError.getArgs.mkString(","), antlrError.charPosition, antlrError.line)

  def parseGrammar(src: String): Either[ParseGrammarFailure, ParseGrammarSuccess] = {
    cache by src.hashCode value {
      val tool = new org.antlr.v4.Tool()
      var errors = Seq[ParseError]()
      tool.removeListeners()
      tool.addListener(new ANTLRToolListener {
        override def warning(msg: ANTLRMessage): Unit = errors = errors :+ convertError(msg)
        override def error(msg: ANTLRMessage): Unit = errors = errors :+ convertError(msg)
        override def info(msg: String): Unit = errors = errors :+ ParseError(msg, 0, 0)
      })

      val grammarRootAst = tool.parseGrammarFromString(src)
      if (!grammarRootAst.hasErrors) {
        val parsedGrammar = tool.createGrammar(grammarRootAst)
        tool.process(parsedGrammar, false)
        Right(ParseGrammarSuccess(parsedGrammar, parsedGrammar.getImplicitLexer, parsedGrammar.getRuleNames))
      }
      else Left(ParseGrammarFailure(errors))
    }
  }
}