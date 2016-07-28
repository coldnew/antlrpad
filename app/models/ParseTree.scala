package models

case class ParseTree(rule: String, text: String, children: Seq[ParseTree])
case class ParseResult(grammar: String, source: String, tree: Option[ParseTree], rules: Seq[String], rule: String, id: Option[Int] = None)