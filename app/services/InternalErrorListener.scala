package services

import org.antlr.v4.tool.{ANTLRMessage, ANTLRToolListener, ErrorManager}
import scala.collection.mutable

case class ParseMessage(message: String, errType: String, col: Int, line: Int)
object ParseMessage {
  val Error = "error"
  val Warning = "warning"
  val Info = "info"
}

class InternalErrorListener(val errorManager: ErrorManager) extends ANTLRToolListener {
  private val allMessages = mutable.MutableList[ParseMessage]()

  def convertError(antlrError: ANTLRMessage, errType: String): ParseMessage = {
    val msg = errorManager.getMessageTemplate(antlrError).render()
    ParseMessage(msg, errType, antlrError.charPosition, antlrError.line)
  }

  override def warning(msg: ANTLRMessage): Unit = allMessages += convertError(msg, ParseMessage.Warning)
  override def error(msg: ANTLRMessage): Unit = allMessages += convertError(msg, ParseMessage.Error)
  override def info(msg: String): Unit = allMessages += ParseMessage(msg, ParseMessage.Info, 0, 0)

  def errors: Seq[ParseMessage] = allMessages.filter(_.errType == ParseMessage.Error)
  def warnings: Seq[ParseMessage] = allMessages.filter(_.errType == ParseMessage.Warning)
}
