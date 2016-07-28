package controllers

import com.google.inject.{Inject, Singleton}
import models.{ParseResult, ParseTree}
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc._
import repo.{ParsedResultsRepository, SavedParseResult}
import services.AntlrParser

import scala.concurrent.Future


@Singleton
class ParserController @Inject() (parser: AntlrParser, val messagesApi: MessagesApi, private val repo: ParsedResultsRepository) extends Controller with I18nSupport {

  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  implicit val treeViewModel = Json.writes[ParseTree]
  implicit val responseModel = Json.writes[ParseResult]
  implicit val parsedResult = Json.writes[SavedParseResult]

  case class FormData(src: String, grammar: String, rule: String)

  val form = Form(mapping(
    "src" -> nonEmptyText,
    "grammar" -> nonEmptyText,
    "rule" -> text
  )(FormData.apply)(FormData.unapply))

  def load(id: Int) = Action.async {
    repo.load(id).map(_ match {
      case Some(record) => Ok(Json.toJson(record))
      case None => NotFound("Cannot find parsed result")
    })
  }

  def parseFromRequest(action: ParseResult => Future[ParseResult])(implicit request: Request[AnyContent]): Future[Result] = {
    form.bindFromRequest.fold(
      formWithErrors => Future { BadRequest(formWithErrors.errorsAsJson.toString()) },
      formData => {
        val result = action(parser.parse(formData.grammar, formData.rule.trim, formData.src))
        result.map(r => Ok(Json.toJson(r)))
      }
    )
  }

  def parseSrc() = Action.async { implicit request=>
    parseFromRequest(r => Future { r })
  }

  def save() = Action.async { implicit request =>
    parseFromRequest(r => repo.save(
      SavedParseResult(r.grammar,
        r.source,
        r.tree.map(t => Json.toJson(t).toString()).getOrElse(""),
        r.rules.mkString(","),
        r.rule,
        None))
      .map(recId => r.copy(id = recId)))
  }
}
