package services

import org.antlr.v4.tool.{Grammar, LexerGrammar}

sealed trait Success
sealed trait Failure

case class Error(msg: String) extends Failure

// handing request
case class RequestSuccess(src: String, grammar: String, lexer: Option[String], rule: String) extends Success
case class RequestFailure(error: String) extends Failure

// grammar parsing results
sealed trait ParseGrammarSuccess extends Success
case class EmptyGrammar() extends ParseGrammarSuccess
case class ParsedGrammar(grammar: Grammar, lexerGrammar: LexerGrammar, rules: Seq[String], warnings: Seq[ParseMessage]) extends ParseGrammarSuccess

case class ParseGrammarFailure(errors: Seq[ParseMessage]) extends Failure

// Test parsing results
case class ParseTree(rule: String, text: String, children: Seq[ParseTree], hasError: Boolean)
case class ParseTextSuccess(tree: ParseTree, rule: String, messages: Seq[ParseMessage], grammar: ParsedGrammar) extends Success
case class ParseTextFailure(error: String) extends Failure

// parse messages for both grammar and expression parsers
case class ParseMessage(source:String, message: String, errType: String, col: Int, line: Int)
object ParseMessage {

  val Error = "error"
  val Warning = "warning"
  val Info = "info"

  val SourceParser = "parser"
  val SourceLexer = "lexer"
  val SourceTextParser = "text"

}
