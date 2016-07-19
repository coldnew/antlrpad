package models

case class ParseTreeViewModel(rule: String, text: String, children: Seq[ParseTreeViewModel])
case class ParseResponseModel(
       tree: ParseTreeViewModel,
       rules: Seq[String],
       id: Int)