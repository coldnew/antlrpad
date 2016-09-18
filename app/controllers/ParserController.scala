package controllers

import com.google.inject.{Inject, Singleton}
import play.Environment
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc._
import repo.{ParsedResultsRepository, SavedParseResult}
import services._
import utils.JsonWriters._

import scala.concurrent.Future
import scalaz.Scalaz._
import scalaz.{-\/, \/, \/-}

@Singleton
class ParserController @Inject() (env: Environment,
                                  val repo: ParsedResultsRepository,
                                  val messagesApi: MessagesApi) extends Controller with I18nSupport {

  import play.api.libs.concurrent.Execution.Implicits.defaultContext

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

  def getResult(output: Failure \/ Success, id: Option[Int] = None): Result = output match {
    case -\/(rf: RequestFailure)      => BadRequest(rf.error)
    case -\/(f: ParseGrammarFailure)  => Ok(Json.toJson(f).asInstanceOf[JsObject] + ("id" -> Json.toJson(id)))
    case \/-(s: ParseTextSuccess)     => Ok(Json.toJson(s).asInstanceOf[JsObject] + ("id" -> Json.toJson(id)))
    case _                            => NotImplemented
  }

  private def getParseResult(request: Request[AnyContent]) = {
    for {
      form    <-  getRequestData(request)
      lexer   <-  LexerGrammarParser(useCache = env.isProd).parse(form.lexer.getOrElse(""))
      parser  <-  GrammarParser(useCache = env.isProd, lexer).parse(form.grammar)
      tree    <-  ExpressionParser(parser).parse(form.src, form.rule)
    } yield tree
  }

  def parseSrc() = Action { implicit request=>
    getResult(getParseResult(request))
  }

  def save() = Action.async { implicit request =>
    getRequestData(request).fold(
      f => Future.successful { getResult(f.left) },
      s => {
        val rec = SavedParseResult(s.grammar, s.lexer.getOrElse(""), s.src, None)
        repo.save(rec).map(id => {
          getResult(getParseResult(request), id)
        })
      }
    )
  }
}
