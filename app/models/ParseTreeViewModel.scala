package models

case class ParseTreeViewModel(rule: String, expr: String, children: Seq[ParseTreeViewModel])