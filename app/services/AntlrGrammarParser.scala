package services

import org.antlr.v4.tool._
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

  def convertError(antlrError: ANTLRMessage)(implicit errorManager: ErrorManager): ParseError = {
    val msg = errorManager.getMessageTemplate(antlrError).render()
    ParseError(msg, antlrError.charPosition, antlrError.line)
  }

  def parseGrammar(src: String): Either[ParseGrammarFailure, ParseGrammarSuccess] = {
    cache by src.hashCode value {
      val tool = new org.antlr.v4.Tool()
      implicit val errorManager = tool.errMgr

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

        if (errors.isEmpty) Right(ParseGrammarSuccess(parsedGrammar, parsedGrammar.getImplicitLexer, parsedGrammar.getRuleNames))
        else Left(ParseGrammarFailure(errors))
      }
      else Left(ParseGrammarFailure(errors))
    }
  }
}