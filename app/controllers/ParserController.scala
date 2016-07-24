package controllers

import com.google.inject.{Inject, Singleton}
import models.{ParseResponseModel, ParseTreeViewModel}
import play.api.data._
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Controller, Request}
import repo.{ParsedResult, ParsedResultsRepository}
import services.AntlrParser

import scala.concurrent.Future


@Singleton
class ParserController @Inject() (parser: AntlrParser, val messagesApi: MessagesApi, private val repo: ParsedResultsRepository) extends Controller with I18nSupport {

  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  implicit val treeViewModel = Json.writes[ParseTreeViewModel]
  implicit val responseModel = Json.writes[ParseResponseModel]
  implicit val parsedResult = Json.writes[ParsedResult]

  val form = Form(tuple(
    "src" -> nonEmptyText,
    "grammar" -> nonEmptyText,
    "rule" -> text
  ))

  def load(id: Int) = Action.async {
    repo.load(id).map(_ match {
      case Some(record) => Ok(Json.toJson(record))
      case None => NotFound("Cannot find parsed result")
    })
  }

  type Error = String
  type Success = (String, String, Seq[String], String, ParseTreeViewModel)

  def parseFromRequest(implicit request: Request[AnyContent]): Either[Error, Success] = {
    form.bindFromRequest.fold(
      formWithErrors => Left(formWithErrors.errorsAsJson.toString()),
      formData => {
        val (src, grammar, rule) = formData
        parser.parse(grammar, rule.trim, src) match {
          case (Some(tree), rules) => Right((grammar, src, rules, rule.trim, tree))
          case _ => Left("There are errors in grammar, source cannot be parsed")
        }
      }
    )
  }

  def parseSrc() = Action { request=>
    parseFromRequest(request) match {
      case Left(err) => BadRequest(err)
      case Right((_, _, rules, rule, tree)) => Ok(Json.toJson(new ParseResponseModel(tree, rules, rule)))
    }
  }

  def save() = Action.async { request =>
    parseFromRequest(request) match {
      case Left(err) => Future { BadRequest(err) }
      case Right((grammar, src, rules, rule, tree)) => {
        repo.save(new ParsedResult(grammar, src, Json.toJson(tree).toString(), rules.mkString(","), rule, None)).map(id => {
          Ok(Json.toJson(new ParseResponseModel(tree, rules, rule, id)))
        })
      }
    }
  }
}
