package models

import services.ParseMessage

case class ParseTree(rule: String, text: String, children: Seq[ParseTree])
case class ParseResult(grammar: String, source: String, tree: Option[ParseTree], rules: Seq[String], rule: String, errors: Seq[ParseMessage], id: Option[Int] = None)

trait Success
trait Failure