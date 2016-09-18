package utils

import play.api.libs.json.{JsValue, Json, Writes}
import services._

object JsonWriters {
  implicit val messageWriter = Json.writes[ParseMessage]
  implicit val grammarWriter = new Writes[ParsedGrammar] {
    override def writes(o: ParsedGrammar): JsValue = {
      Json.obj("rules" -> o.rules, "warnings" -> Json.toJson(o.warnings))
    }
  }

  implicit val grammarFailureWriter = new Writes[ParseGrammarFailure] {
    override def writes(o: ParseGrammarFailure): JsValue = {
      Json.obj("grammar" -> Json.obj("errors" -> o.errors))
    }
  }

  implicit val treeWriter = Json.writes[ParseTree]
  implicit val successWriter = Json.writes[ParseTextSuccess]
}
