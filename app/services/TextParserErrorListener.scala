package services

import org.antlr.v4.runtime.{BaseErrorListener, RecognitionException, Recognizer}

import scala.collection.mutable


class TextParserErrorListener extends BaseErrorListener {
  val allMessages = mutable.MutableList[ParseMessage]()

  override def syntaxError(recognizer: Recognizer[_, _], offendingSymbol: scala.Any, line: Int, charPositionInLine: Int, msg: String, e: RecognitionException): Unit = {
    allMessages += ParseMessage(ParseMessage.SourceTextParser, msg, ParseMessage.Error, charPositionInLine, line)
    super.syntaxError(recognizer, offendingSymbol, line, charPositionInLine, msg, e)
  }

  def errors: Seq[ParseMessage] = allMessages.filter(_.errType == ParseMessage.Error)
}
