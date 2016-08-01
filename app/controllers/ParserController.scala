package controllers

import com.google.inject.{Inject, Singleton}
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc._
import repo.{ParsedResultsRepository, SavedParseResult}
import services.{AntlrGrammarParser, AntlrTextParser, ParseGrammarFailure, ParseTree}

import scala.concurrent.Future
import scalaz.Scalaz._
import scalaz._

@Singleton
class ParserController @Inject() (parser: AntlrTextParser, grammarParser: AntlrGrammarParser, val messagesApi: MessagesApi, private val repo: ParsedResultsRepository) extends Controller with I18nSupport {

  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  implicit val treeResult = Json.writes[ParseTree]
  implicit val successResult = Json.writes[ParseSuccess]
  implicit val loadSuccess = Json.writes[LoadSuccess]
  implicit val saveSuccess = Json.writes[SaveSuccess]

  case class FormData(src: String, grammar: String, rule: String)

  case class ParseSuccess(rules: Seq[String], tree: ParseTree, rule: String, id: Option[Int] = None)
  case class SaveSuccess(id: Option[Int])
  case class LoadSuccess(grammar: String, src: String, rules: Seq[String], tree: Option[ParseTree], rule: String)
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

  def getResult[L, R](parsedResult: \/[L, R]): Result = parsedResult match {
    case \/-(t: ParseSuccess) => Ok(Json.toJson(t))
    case \/-(t: SaveSuccess) => Ok(Json.toJson(t))
    case \/-(t: LoadSuccess) => Ok(Json.toJson(t))
    case -\/(e: ParseGrammarFailure) => BadRequest(e.errors.mkString(","))
    case _ => BadRequest("Failed")
  }

  def parseSrc() = Action { implicit request=>
    val parsedResult = for(
      req       <- parseRequest;
      grammars  <- grammarParser.parseGrammar(req.grammar);
      tree      <- parser.parse(req.src, req.rule)(grammars);
      res       <- ParseSuccess(grammars.rules, tree, req.rule).right
    ) yield res

    getResult(parsedResult)
  }

  def saveToDb(data: FormData) = {
    repo.save(SavedParseResult(data.grammar, data.src, "", "", data.rule, None))
  }

  def save() = Action.async { implicit request =>
    val result = for(
      req       <- parseRequest;
      saved     <- saveToDb(req).right
    ) yield saved

    result.fold(
      l => Future { getResult(l.left) },
      r => r.map(id => {
        val parsedResult = for(
          req       <- parseRequest;
          grammars  <- grammarParser.parseGrammar(req.grammar);
          tree      <- parser.parse(req.src, req.rule)(grammars);
          res       <- ParseSuccess(grammars.rules, tree, req.rule, id).right
        ) yield res

        getResult(parsedResult ||| SaveSuccess(id).right)
      })
    )
  }

  def load(id: Int) = Action.async {
    repo.load(id).map(_ match {
      case Some(record) => {
        val parsedResult = for(
          grammars <- grammarParser.parseGrammar(record.grammar);
          tree <- parser.parse(record.src, record.rule)(grammars);
          res <- LoadSuccess(record.grammar, record.src, grammars.rules, Some(tree), record.rule).right
        ) yield res

        getResult(parsedResult ||| LoadSuccess(record.grammar, record.src, Seq.empty, None, "").right)
      }
      case None => NotFound("Cannot find parsed result")
    })
  }
}
