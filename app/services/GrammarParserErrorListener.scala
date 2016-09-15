package services

import org.antlr.v4.tool.{ANTLRMessage, ANTLRToolListener, ErrorManager}
import scala.collection.mutable

class GrammarParserErrorListener(val errorManager: ErrorManager, val source: String) extends ANTLRToolListener {
  private val allMessages = mutable.MutableList[ParseMessage]()

  def convertError(antlrError: ANTLRMessage, errType: String): ParseMessage = {
    val msg = errorManager.getMessageTemplate(antlrError).render()
    ParseMessage(source, msg, errType, antlrError.charPosition, antlrError.line)
  }

  override def warning(msg: ANTLRMessage): Unit = allMessages += convertError(msg, ParseMessage.Warning)
  override def error(msg: ANTLRMessage): Unit = allMessages += convertError(msg, ParseMessage.Error)
  def error(msg: String): Unit = allMessages += ParseMessage(source, msg, ParseMessage.Error, 0, 0)
  override def info(msg: String): Unit = allMessages += ParseMessage(source, msg, ParseMessage.Info, 0, 0)

  // Check for error message text is just a hack to ignore silly ANTLR behaviour - ALWAYS check if tokens file exists even
  // if vocab has been imported already. When parser includes lexer and parser (i.e. it is not a combined grammar) LG is parsed
  // first and then imported into parser grammar. So there is no need in token file, obviously.
  def all: Seq[ParseMessage] = allMessages.filter(!_.message.contains("cannot find tokens file"))

  def errors: Seq[ParseMessage] = all.filter(_.errType == ParseMessage.Error)
  def warnings: Seq[ParseMessage] = all.filter(_.errType == ParseMessage.Warning)
}
