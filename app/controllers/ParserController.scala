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

  case class ParseRequest(src: String, grammar: String, lexer: Option[String], rule: String)

  private val form = Form(mapping(
    "src" -> nonEmptyText,
    "grammar" -> nonEmptyText,
    "lexer" -> optional(text),
    "rule" -> text
  )(ParseRequest.apply)(ParseRequest.unapply))

  private def getResult(output: Failure \/ Success, id: Option[String] = None): Result = output match {
    case -\/(f: ParseGrammarFailure)  => Ok(Json.toJson(f).asInstanceOf[JsObject] + ("id" -> Json.toJson(id)))
    case \/-(s: ParseTextSuccess)     => Ok(Json.toJson(s).asInstanceOf[JsObject] + ("id" -> Json.toJson(id)))
    case _                            => NotImplemented
  }

  private def getParseResult(form: ParseRequest) = {
    for {
      lexer   <-  LexerGrammarParser(useCache = env.isProd).parse(form.lexer.getOrElse(""))
      parser  <-  GrammarParser(useCache = env.isProd, lexer).parse(form.grammar)
      tree    <-  ExpressionParser(parser).parse(form.src, form.rule)
    } yield tree
  }

  // this is to allow use withValidRequest in async actions and wrap BadRequest into Future
  implicit val wrapResult: Result => Future[Result] = Future.successful

  private def withValidRequest[T](request: Request[AnyContent])(body: ParseRequest => T)(implicit wrap: Result => T): T = {
    form.bindFromRequest()(request).fold(
      formWithErrors => wrap(BadRequest(formWithErrors.errorsAsJson.toString())),
      formData => body(formData)
    )
  }

  def parseExpr() = Action { request=>
    withValidRequest[Result](request) { data =>
      getResult(getParseResult(data))
    }
  }

  def save() = Action.async { request =>
    withValidRequest(request) { data =>
      val codeGenerator = new CodeGenerator(6, 10, new DbCodeValidator(repo))
      codeGenerator.getNewCode.flatMap {
        case -\/(Error(msg)) => Future.successful(BadRequest(msg))
        case \/-(code) => {
          val rec = SavedParseResult(data.grammar, data.lexer.getOrElse(""), data.src, code,  None)
          repo.save(rec).map(id => {
            getResult(getParseResult(data), Option(id))
          })
        }
      }
    }
  }

  def load(id: String) = Action.async {
    repo.load(id).map {
      case Some(record) => Ok(Json.toJson(record))
      case None => NotFound("Cannot find record")
    }
  }
}
