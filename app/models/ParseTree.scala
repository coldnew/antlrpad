package models

import services.ParseError

case class ParseTree(rule: String, text: String, children: Seq[ParseTree])
case class ParseResult(grammar: String, source: String, tree: Option[ParseTree], rules: Seq[String], rule: String, errors: Seq[ParseError], id: Option[Int] = None)