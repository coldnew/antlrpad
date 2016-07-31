package controllers

import com.google.inject.{Inject, Singleton}
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc._
import repo.{ParsedResultsRepository, SavedParseResult}
import services.{AntlrGrammarParser, AntlrTextParser, ParseGrammarFailure, ParseTree}

import scalaz.Scalaz._
import scalaz._

@Singleton
class ParserController @Inject() (parser: AntlrTextParser, grammarParser: AntlrGrammarParser, val messagesApi: MessagesApi, private val repo: ParsedResultsRepository) extends Controller with I18nSupport {

  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  implicit val treeResult = Json.writes[ParseTree]
  implicit val parsedResult = Json.writes[SavedParseResult]

  case class FormData(src: String, grammar: String, rule: String)
  case class RequestFailure(error: String)

  val form = Form(mapping(
    "src" -> nonEmptyText,
    "grammar" -> nonEmptyText,
    "rule" -> text
  )(FormData.apply)(FormData.unapply))

  def parseRequest(implicit request: Request[AnyContent]): \/[RequestFailure, FormData] = {
    form.bindFromRequest.fold(
      formWithErrors => RequestFailure(formWithErrors.errorsAsJson.toString()).left,
      formData => formData.right
    )
  }

  def getResult[L, R](parsedResult: \/[L, R]) = parsedResult match {
    case \/-(t: ParseTree) => Ok(Json.toJson(t))
    case -\/(e: ParseGrammarFailure) => BadRequest(e.errors.mkString(","))
    case _ => BadRequest("Failed")
  }

  def parseSrc() = Action { implicit request=>
    val parsedResult = for(
      req <- parseRequest;
      grammars <- grammarParser.parseGrammar(req.grammar);
      res <- parser.parse(req.src, req.rule)(grammars)
    ) yield res

    getResult(parsedResult)
  }

  def save() = Action { implicit request =>
    Ok("done")
  }

  def load(id: Int) = Action.async {
    repo.load(id).map(_ match {
      case Some(record) => Ok(Json.toJson(record))
      case None => NotFound("Cannot find parsed result")
    })
  }
}
