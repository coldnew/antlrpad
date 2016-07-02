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
    val postData = request.body.asFormUrlEncoded.getOrElse(Map.empty)

    val grammarSrc = postData.getOrElse("grammar", Seq.empty).headOption.orNull
    val src = postData.getOrElse("src", Seq.empty).headOption.orNull
    val rule = postData.getOrElse("rule", Seq.empty).headOption.orNull

    (grammarSrc, src) match {
      case (null, _) => BadRequest("src parameter must be specified, content should be form encoded")
      case (_, null) => BadRequest("grammar parameter must be specified, content should be form encoded")
      case (grammarSrc, src) => {
        val (g, lg) = new GrammarParser().parseGrammar(grammarSrc)
        val tree = new TextParser().parse(src, rule, g, lg)

        Ok(Json.toJson(tree))
      }
    }
  }

}
