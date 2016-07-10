package controllers

import com.google.inject.{Inject, Singleton}
import models.{ParseResponseModel, ParseTreeViewModel}
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import services.AntlrParser

@Singleton
class ParserController @Inject() (parser: AntlrParser) extends Controller {

  implicit val treeViewModel = Json.writes[ParseTreeViewModel]
  implicit val responseModel = Json.writes[ParseResponseModel]

  def parseSrc() = Action { request =>
    val postData = request.body.asFormUrlEncoded.getOrElse(Map.empty)

    val grammarSrc = postData.getOrElse("grammar", Seq.empty).headOption.orNull
    val src = postData.getOrElse("src", Seq.empty).headOption.orNull
    val rule = postData.getOrElse("rule", Seq.empty).headOption.orNull

    (grammarSrc, src) match {
      case (null, _) => BadRequest("src parameter must be specified, content should be form encoded")
      case (_, null) => BadRequest("grammar parameter must be specified, content should be form encoded")
      case (grammarSrc, src) => {
        parser.parse(grammarSrc, rule.trim, src) match {
          case (Some(t), rules) => Ok(Json.toJson(ParseResponseModel(t, rules)))
          case _ => BadRequest("There are errors in grammar, source cannot be parsed")
        }
      }
    }
  }

}
