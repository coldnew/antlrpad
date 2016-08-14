package services

case class ParseMessage(message: String, errType: String, col: Int, line: Int)
object ParseMessage {
  val Error = "error"
  val Warning = "warning"
  val Info = "info"
}
