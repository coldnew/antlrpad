package controllers

import com.google.inject.{Inject, Singleton}
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc._
import repo.{ParsedResultsRepository, SavedParseResult}
import services.{AntlrGrammarParser, AntlrTextParser, ParseGrammarFailure, ParseTree}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scalaz.Scalaz._
import scalaz._

@Singleton
class ParserController @Inject() (parser: AntlrTextParser, grammarParser: AntlrGrammarParser, val messagesApi: MessagesApi, private val repo: ParsedResultsRepository) extends Controller with I18nSupport {

  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  implicit val treeResult = Json.writes[ParseTree]
  implicit val successResult = Json.writes[ParseSuccess]
  implicit val parsedResult = Json.writes[SavedParseResult]

  case class FormData(src: String, grammar: String, rule: String)
  case class ParseSuccess(rules: Seq[String], tree: ParseTree, rule: String, id: Option[Int] = None)
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

  def getResult[L, R](parsedResult: \/[L, R]): Future[Result] = parsedResult match {
    case \/-(t: ParseSuccess) => Future { Ok(Json.toJson(t)) }
    case \/-(f: Future[ParseSuccess]) => f.map(res => Ok(Json.toJson(res)))
    case -\/(e: ParseGrammarFailure) => Future { BadRequest(e.errors.mkString(",")) }
    case _ => Future { BadRequest("Failed") }
  }

  def parseSrc() = Action.async { implicit request=>
    val parsedResult = for(
      req <- parseRequest;
      grammars <- grammarParser.parseGrammar(req.grammar);
      tree <- parser.parse(req.src, req.rule)(grammars);
      res <- ParseSuccess(grammars.rules, tree, req.rule).right
    ) yield res

    getResult(parsedResult)
  }

  def saveToDb(data: FormData) = {
    repo.save(SavedParseResult(data.grammar, data.src, "", "", data.rule, None)).right
  }

  def save() = Action.async { implicit request =>
    val parsedResult = for(
      req <- parseRequest;
      saved <- saveToDb(req);
      grammars <- grammarParser.parseGrammar(req.grammar);
      tree <- parser.parse(req.src, req.rule)(grammars);
      res <- saved.map(id => ParseSuccess(grammars.rules, tree, req.rule, id)).right
    ) yield res

    getResult(parsedResult)
  }

  def load(id: Int) = Action.async {
    repo.load(id).map(_ match {
      case Some(record) => Ok(Json.toJson(record))
      case None => NotFound("Cannot find parsed result")
    })
  }
}
