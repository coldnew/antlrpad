package controllers

import com.google.inject.{Inject, Singleton}
import play.Environment
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc._
import repo.ParsedResultsRepository
import services._
import utils.JsonWriters._

import scala.concurrent.Future
import scalaz.Scalaz._
import scalaz.{-\/, \/, \/-}

@Singleton
class ParserController @Inject() (env: Environment,
                                  private val repo: ParsedResultsRepository,
                                  val messagesApi: MessagesApi) extends Controller with I18nSupport {

  val form = Form(mapping(
    "src" -> nonEmptyText,
    "grammar" -> nonEmptyText,
    "lexer" -> optional(text),
    "rule" -> text
  )(RequestSuccess.apply)(RequestSuccess.unapply))

  def getRequestData(implicit request: Request[AnyContent]): RequestFailure \/ RequestSuccess  = {
    form.bindFromRequest.fold(
      formWithErrors => RequestFailure(formWithErrors.errorsAsJson.toString()).left,
      formData => formData.right
    )
  }

  def load(id: Int) = Action.async {
    Future.successful(Ok("{}"))
  }

  def getResult(output: Failure \/ Success): Result = output match {
    case -\/(rf: RequestFailure) => BadRequest(rf.error)
    case -\/(f: ParseGrammarFailure) => Ok(Json.toJson(f))
    case \/-(s: ParseTextSuccess) => Ok(Json.toJson(s))
    case _ => NotImplemented
  }

  def parseSrc() = Action { implicit request=>
    getResult(for {
      form    <-  getRequestData
      lexer   <-  LexerGrammarParser(useCache = env.isProd).parse(form.lexer.getOrElse(""))
      parser  <-  GrammarParser(useCache = env.isProd, lexer).parse(form.grammar)
      tree    <-  ExpressionParser(parser).parse(form.src, form.rule)
    } yield tree)
  }

  def save() = Action.async { implicit request =>
    Future.successful(Ok("{}"))
  }
}
