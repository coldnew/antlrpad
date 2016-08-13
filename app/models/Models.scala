package models

import play.api.libs.json.{JsValue, Json}
import services.ParseMessage

case class ParseTree(rule: String, text: String, children: Seq[ParseTree])
case class ParseResult(grammar: String, source: String, tree: Option[ParseTree], rules: Seq[String], rule: String, errors: Seq[ParseMessage], id: Option[Int] = None)

trait Success
trait Failure