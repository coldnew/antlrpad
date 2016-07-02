package controllers

import com.google.inject.Singleton
import models.ParseTreeViewModel
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import services.{GrammarParser, TextParser}

@Singleton
class ParserController extends Controller {

  implicit val treeViewModel = Json.writes[ParseTreeViewModel]

  def parseSrc() = Action { request =>
    val grammarSrc = request.body.asFormUrlEncoded.get("grammar").head
    val src = request.body.asFormUrlEncoded.get("src").head
    val rule = request.body.asFormUrlEncoded.find(_ == "rule").getOrElse("").toString

    val (g, lg) = new GrammarParser().parseGrammar(grammarSrc)
    val tree = new TextParser().parse(src, rule, g, lg)

    Ok(Json.toJson(tree))
  }

}
