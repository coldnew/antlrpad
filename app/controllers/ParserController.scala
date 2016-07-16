package controllers

import com.google.inject.{Inject, Singleton}
import models.{ParseResponseModel, ParseTreeViewModel}
import play.api.data._
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import services.AntlrParser

@Singleton
class ParserController @Inject() (parser: AntlrParser, val messagesApi: MessagesApi) extends Controller with I18nSupport {

  implicit val treeViewModel = Json.writes[ParseTreeViewModel]
  implicit val responseModel = Json.writes[ParseResponseModel]

  val form = Form(tuple(
    "src" -> nonEmptyText,
    "grammar" -> nonEmptyText,
    "rule" -> text
  ))

  def parseSrc() = Action { implicit request =>

    form.bindFromRequest.fold(
      formWithErrors => BadRequest(formWithErrors.errorsAsJson),
      formData => {
        val (src, grammar, rule) = formData
        parser.parse(grammar, rule.trim, src) match {
          case (Some(t), rules) => Ok(Json.toJson(ParseResponseModel(t, rules)))
          case _ => BadRequest("There are errors in grammar, source cannot be parsed")
        }
      }
    )
  }
}
