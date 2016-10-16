package controllers

import com.google.inject.{Inject, Singleton}
import play.Environment
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc._
import repo.{DbCodeValidator, ParsedResultsRepository, SavedParseResult}
import services._
import utils.JsonWriters._

import scala.concurrent.Future
import scalaz.Scalaz._
import scalaz.{-\/, \/, \/-}

@Singleton
class ParserController @Inject() (env: Environment,
                                  repo: ParsedResultsRepository,
                                  val messagesApi: MessagesApi) extends Controller with I18nSupport {

  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  private val form = Form(mapping(
    "src" -> nonEmptyText,
    "grammar" -> nonEmptyText,
    "lexer" -> optional(text),
    "rule" -> text
  )(RequestSuccess.apply)(RequestSuccess.unapply))

  private def getRequestData(implicit request: Request[AnyContent]): RequestFailure \/ RequestSuccess  = {
    form.bindFromRequest.fold(
      formWithErrors => RequestFailure(formWithErrors.errorsAsJson.toString()).left,
      formData => formData.right
    )
  }

  private def getResult(output: Failure \/ Success, id: Option[String] = None): Result = output match {
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

  def parseExpr() = Action { implicit request=>
    getResult(getParseResult(request))
  }

  def save() = Action.async { implicit request =>
    val codeGenerator = new CodeGenerator(6, 10, new DbCodeValidator(repo))

    getRequestData(request).fold(
      f => Future.successful { getResult(f.left) },
      s => {
        codeGenerator.getNewCode.flatMap {
          case -\/(Error(msg)) => Future.successful(BadRequest(msg))
          case \/-(code) => {
            val rec = SavedParseResult(s.grammar, s.lexer.getOrElse(""), s.src, code,  None)
            repo.save(rec).map(id => {
              getResult(getParseResult(request), Option(id))
            })
          }
        }
      }
    )
  }

  def load(id: String) = Action.async {
    repo.load(id).map {
      case Some(record) => Ok(Json.toJson(record))
      case None => NotFound("Cannot find record")
    }
  }
}
