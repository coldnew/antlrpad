package controllers

import com.google.inject.{Inject, Singleton}
import models.{Failure, ParseTree, Success}
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json._
import play.api.mvc._
import repo.{ParsedResultsRepository, SavedParseResult}
import services.{ParseMessage, _}

import scala.concurrent.Future
import scalaz.Scalaz._
import scalaz.{-\/, \/, \/-}

@Singleton
class ParserController @Inject() (grammarParser: AntlrGrammarParser,
                                  private val repo: ParsedResultsRepository,
                                  val messagesApi: MessagesApi) extends Controller with I18nSupport {

  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  implicit lazy val parseMessageWriter = Json.writes[ParseMessage]
  implicit lazy val parseTextSuccess = new Writes[ParseGrammarSuccess]{
    override def writes(o: ParseGrammarSuccess): JsValue = {
      Json.obj("rules" -> o.rules, "warnings" -> Json.toJson(o.warnings))
    }
  }

  implicit lazy val savedParseResultWriter = Json.writes[SavedParseResult]
  implicit lazy val parseTreeWriter = Json.writes[ParseTree]
  implicit lazy val successWriter = Json.writes[ParseTextSuccess]
  implicit lazy val parseGrammarFailureWriter = new Writes[ParseGrammarFailure] {
    override def writes(o: ParseGrammarFailure): JsValue = {
      Json.obj("grammar" -> Json.obj("errors" -> o.errors))
    }
  }

  case class RequestData(src: String, grammar: String, lexer: Option[String], rule: String) extends Success
  case class RequestFailure(error: String) extends Failure
  case class SaveRequestResult(saveRequest: Future[Option[Int]], result: Failure \/ Success)

  val form = Form(mapping(
    "src" -> nonEmptyText,
    "grammar" -> nonEmptyText,
    "lexer" -> optional(text),
    "rule" -> text
  )(RequestData.apply)(RequestData.unapply))

  def getRequestData(implicit request: Request[AnyContent]): RequestFailure \/ RequestData  = {
    form.bindFromRequest.fold(
      formWithErrors => RequestFailure(formWithErrors.errorsAsJson.toString()).left,
      formData => formData.right
    )
  }

  def load(id: Int) = Action.async {
    repo.load(id).map {
      case Some(record) => Ok(Json.toJson(record))
      case None => NotFound("Cannot find parsed result")
    }
  }

  def getResult(res: Failure \/ Success, id: Option[Int] = None): Result = {
    res match {
      case -\/(failure: RequestFailure) => BadRequest(failure.error)
      case -\/(failure: ParseGrammarFailure) => Ok(Json.toJson(failure))
      case \/-(success: ParseTextSuccess) => {
        val json = Json.toJson(success).as[JsObject] + ("id" -> Json.toJson(id))
        Ok(json)
      }
      case _ => BadRequest("Cannot process result now")
    }
  }

  def parseSrc() = Action { implicit request=>
    val res = for {
      req   <-  getRequestData
      grm   <-  grammarParser.parseGrammar(req.grammar, req.lexer)
      exp   <-  new AntlrTextParser(grm).parse(req.src, req.rule)
    } yield exp

    getResult(res)
  }

  def save() = Action.async { implicit request =>
    val saveRes = for {
      req   <-  getRequestData
      id    =   repo.save(SavedParseResult(req.grammar, req.lexer.getOrElse(""), req.src, None))
      grm   <-  grammarParser.parseGrammar(req.grammar, req.lexer)
      exp   =   new AntlrTextParser(grm).parse(req.src, req.rule)
    } yield SaveRequestResult(id, exp)

    saveRes.fold(
      err => Future.successful(getResult(err.left)),
      res => res.saveRequest.map(id => getResult(res.result, id))
    )
  }
}
