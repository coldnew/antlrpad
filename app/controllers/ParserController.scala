package controllers

import com.google.inject.{Inject, Singleton}
import models.{ParseResponseModel, ParseTreeViewModel}
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import services.AntlrParser

@Singleton
class ParserController @Inject() (parser: AntlrParser) extends Controller {

  implicit val treeViewModel = Json.writes[ParseTreeViewModel]
  implicit val responseModel = Json.writes[ParseResponseModel]

  case class ParseRequest(src: String, grammar: String, rule: String)

  val form = Form(mapping(
    "src" -> text,
    "grammar" -> text,
    "rule" -> text
  )(ParseRequest.apply)(ParseRequest.unapply))

  def parseSrc() = Action { implicit request =>
    form.bindFromRequest.fold(
      formWithErrors => BadRequest(formWithErrors.errors.map(_.message).mkString(", ")),
      parsedForm => parsedForm match {
        case ParseRequest(null, _, _) => BadRequest("src parameter must be specified, content should be form encoded")
        case ParseRequest(_, null, _) => BadRequest("grammar parameter must be specified, content should be form encoded")
        case ParseRequest(src, grammarSrc, rule) => {
          parser.parse(grammarSrc, rule.trim, src) match {
            case (Some(t), rules) => Ok(Json.toJson(ParseResponseModel(t, rules)))
            case _ => BadRequest("There are errors in grammar, source cannot be parsed")
          }
        }
      })
    }

}
