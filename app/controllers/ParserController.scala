package controllers

import com.google.inject.{Inject, Singleton}
import models.{Failure, ParseResult, Success}
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc._
import repo.{ParsedResultsRepository, SavedParseResult}
import services.{AntlrGrammarParser, AntlrTextParser}

import scala.concurrent.Future
import scalaz.{-\/, \/, \/-}
import scalaz.Scalaz._

@Singleton
class ParserController @Inject() (grammarParser: AntlrGrammarParser,
                                  parser: AntlrTextParser,
                                  private val repo: ParsedResultsRepository,
                                  val messagesApi: MessagesApi) extends Controller with I18nSupport {

  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  implicit lazy val savedParseResultWriter = Json.writes[SavedParseResult]

  case class RequestData(src: String, grammar: String, rule: String) extends Success
  case class RequestFailure(error: String) extends Failure
  case class SaveRequestResult(saveRequst: Future[Option[Int]], result: Failure \/ Success)

  val form = Form(mapping(
    "src" -> nonEmptyText,
    "grammar" -> nonEmptyText,
    "rule" -> text
  )(RequestData.apply)(RequestData.unapply))

  def getRequestData(implicit request: Request[AnyContent]): RequestFailure \/ RequestData  = {
    form.bindFromRequest.fold(
      formWithErrors => RequestFailure(formWithErrors.errorsAsJson.toString()).left,
      formData => formData.right
    )
  }

  def load(id: Int) = Action.async {
    repo.load(id).map(_ match {
      case Some(record) => Ok(Json.toJson(record))
      case None => NotFound("Cannot find parsed result")
    })
  }

  def getResult(res: Failure \/ Success): Result = {
    res match {
      case -\/(failure: RequestFailure) => BadRequest(failure.error)
      case -\/(failure: Failure) => Ok("failed")
      case \/-(success: Success) => Ok("success")
    }
  }

  def parseSrc() = Action { implicit request=>
    val res = for {
      req   <-  getRequestData
      grm   <-  grammarParser.parseGrammar(req.grammar)
      exp   <-  parser.parse(req.src, req.rule, grm.grammar, grm.lexerGrammar)
    } yield exp

    getResult(res)
  }

  def save() = Action.async { implicit request =>
    val saveRes = for {
      req   <-  getRequestData
      id    =   repo.save(SavedParseResult(req.grammar, req.src, "", "", "", None))
      grm   <-  grammarParser.parseGrammar(req.grammar)
      exp   =   parser.parse(req.src, req.rule, grm.grammar, grm.lexerGrammar)
    } yield SaveRequestResult(id, exp)

    saveRes.fold(
      err => Future.successful(getResult(err.left)),
      res => res.saveRequst.map(id => getResult(res.result))
    )
  }
}
