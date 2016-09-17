package services

case class ParseMessage(source:String, message: String, errType: String, col: Int, line: Int)
object ParseMessage {

  val Error = "error"
  val Warning = "warning"
  val Info = "info"

  val SourceParser = "parser"
  val SourceLexer = "lexer"
  val SourceTextParser = "text"

}