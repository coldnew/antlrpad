package controllers

import com.google.inject.{Inject, Singleton}
import models.{ParseResponseModel, ParseTreeViewModel}
import play.api.data._
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import repo.{ParsedResult, ParsedResultsRepository}
import services.AntlrParser

import scala.concurrent.Future

@Singleton
class ParserController @Inject() (parser: AntlrParser, val messagesApi: MessagesApi, private val repo: ParsedResultsRepository) extends Controller with I18nSupport {

  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  implicit val treeViewModel = Json.writes[ParseTreeViewModel]
  implicit val responseModel = Json.writes[ParseResponseModel]

  val form = Form(tuple(
    "src" -> nonEmptyText,
    "grammar" -> nonEmptyText,
    "rule" -> text
  ))

  def parseSrc() = Action.async { implicit request =>

    form.bindFromRequest.fold(
      formWithErrors => Future {
        BadRequest(formWithErrors.errorsAsJson)
      },
      formData => {
        val (src, grammar, rule) = formData
        parser.parse(grammar, rule.trim, src) match {
          case (Some(t), rules) => {
            val record = new ParsedResult(grammar, src, "", None)
            repo.insert(record).map(i => Ok(Json.toJson(ParseResponseModel(t, rules, i))))
          }
          case _ => Future{ BadRequest("There are errors in grammar, source cannot be parsed") }
        }
      }
    )
  }
}
