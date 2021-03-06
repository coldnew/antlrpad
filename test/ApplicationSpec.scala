import akka.stream.Materializer
import org.scalatest.{Ignore, TestData}
import org.scalatestplus.play._
import play.api.Application
import play.api.libs.json._
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test._

import scala.concurrent.Future

class ApplicationSpec extends PlaySpec with OneAppPerTest with AntlrFakeApp {

  val MISMATCHED_EOF = """[{"source":"parser","message":"error(50): :2:9: syntax error: mismatched input '<EOF>' expecting SEMI while matching a rule","errType":"error","col":9,"line":2}]"""
  val RULE_CAN_MATCH_EMPTY_STRING = """[{"source":"parser","message":"warning(146): :2:9: non-fragment lexer rule ID can match the empty string","errType":"warning","col":9,"line":2}]"""
  val A_LETTER_PARSE_TREE = """{"rule":"a","text":"id","children":[],"hasError":false}"""
  val LEXER_ERROR = """[{"source":"lexer","message":"error(50): :2:9: syntax error: '<EOF>' came as a complete surprise to me while matching a lexer rule","errType":"error","col":9,"line":2}]"""
  val LEXER_WARNING = """[{"source":"lexer","message":"warning(146): :2:1: non-fragment lexer rule ID can match the empty string","errType":"warning","col":1,"line":2}]"""

  implicit lazy val materializer: Materializer = app.materializer

  override def newAppForTest(testData: TestData): Application = antlrFakeApp(testData)

  def sendParseRequest(data: (String, String)*): Future[Result] = {
    route(app, FakeRequest(POST, "/api/parse/").withFormUrlEncodedBody(data: _*)).get
  }

  def sendSaveRequest(data: (String, String)*): Future[Result] = {
    route(app, FakeRequest(POST, "/api/save/").withFormUrlEncodedBody(data: _*)).get
  }

  def sendLoadRequest(id: String): Future[Result] = {
    route(app, FakeRequest(GET, s"/api/load/$id")).get
  }

  "Routes" should {

    "send 404 on a non-existing page" in  {
      route(app, FakeRequest(GET, "/boum")).map(status) mustBe Some(NOT_FOUND)
    }

  }

  "Index page " should {

    "give static page " in {
      val home = route(app, FakeRequest(GET, "/")).get
      status(home) mustBe OK
      contentAsString(home) must include("<title>AntlrPad</title>")
    }

  }

  "ParseController" should {

    "give bad request when no grammar " in {
      val home = route(app, FakeRequest(POST, "/api/parse/")).get
      status(home) mustBe BAD_REQUEST
    }

    "return combined grammar errors" in {
      val response = sendParseRequest(
        ("grammar", "grammar test; \n two: '2'"),
        ("src", "2"),
        ("rule", ""))

      status(response) mustBe OK
      contentAsJson(response) \ "parsed" \ "grammar" \ "errors" mustBe JsDefined(Json.parse(MISMATCHED_EOF))
    }

    "return combined grammar warnings" in {
      val response = sendParseRequest(
        ("grammar", "grammar test; \n id: ID; ID: 'a'*;"),
        ("src", "2"),
        ("rule", "")
      )

      status(response) mustBe OK
      contentAsJson(response) \ "parsed" \ "grammar" \ "warnings" mustBe JsDefined(Json.parse(RULE_CAN_MATCH_EMPTY_STRING))
    }

    "use lexer grammar when provided" in {
      val response = sendParseRequest(
        ("grammar", "grammar test; \n id: ID; "),
        ("lexer", "lexer grammar test; \n ID: 'a'*;"),
        ("src", "a"),
        ("rule", ""))

      status(response) mustBe OK
      contentAsJson(response) \ "parsed" \ "tree" mustBe JsDefined(Json.parse(A_LETTER_PARSE_TREE))
    }

    "return lexer grammar errors" in {
      val response = sendParseRequest(
        ("grammar", "grammar test; \n id: ID; "),
        ("lexer", "lexer grammar test; \n ID: 'a'*"),
        ("src", "a"),
        ("rule", ""))

      status(response) mustBe OK
      contentAsJson(response) \ "parsed" \ "grammar" \ "errors" mustBe JsDefined(Json.parse(LEXER_ERROR))
    }

    "return lexer grammar warnings" in {
      val response = sendParseRequest(
        ("grammar", "parser grammar test; \n id: ID; "),
        ("lexer", "lexer grammar test; \n ID: 'a'*;"),
        ("src", "a"),
        ("rule", ""))

      status(response) mustBe OK
      contentAsJson(response) \ "parsed" \ "grammar" \ "warnings" mustBe JsDefined(Json.parse(LEXER_WARNING))
    }

    "use implicit lexer for combined grammar" in {
      val response = sendParseRequest(
        ("grammar", "grammar test; \n id: ID; ID: 'a'*;"),
        ("src", "a"),
        ("rule", ""))

      status(response) mustBe OK
      contentAsJson(response) \ "parsed" \ "tree" mustBe JsDefined(Json.parse(A_LETTER_PARSE_TREE))
    }

    "return saved record ID for incorrect grammar" in {
      val response = sendSaveRequest(
        ("grammar", "grammar test; \n id: ID; ID: 'a'*"),
        ("src", "a"),
        ("rule", ""))

      status(response) mustBe OK
      contentAsJson(response) \ "saved" \"id" mustBe JsDefined(JsString("abcdef"))
    }

    "return saved record ID for incorrect lexer" in {
      val response = sendSaveRequest(
        ("grammar", "grammar test; \n id: ID;"),
        ("lexer", "lexer grammar test; ID: 'a'*"),
        ("src", "a"),
        ("rule", ""))

      status(response) mustBe OK
      contentAsJson(response) \ "saved" \"id" mustBe JsDefined(JsString("abcdef"))
    }

    "return saved record ID for parsed tree" in {
      val response = sendSaveRequest(
        ("grammar", "grammar test; \n id: ID;"),
        ("lexer", "lexer grammar test; ID: 'a'*;"),
        ("src", "a"),
        ("rule", ""))

      status(response) mustBe OK
      contentAsJson(response) \ "saved" \ "id" mustBe JsDefined(JsString("abcdef"))
    }

    "load saved record by id" in {
      val response = sendLoadRequest("abcdef")

      status(response) mustBe OK

      contentAsJson(response) \ "loaded" \ "src" mustBe JsDefined(JsString("id"))
      contentAsJson(response) \ "loaded" \ "code" mustBe JsDefined(JsString("abcdef"))
      contentAsJson(response) \ "loaded" \ "grammarSrc" mustBe JsDefined(JsString("grammar test; id: ID+;"))
      contentAsJson(response) \ "loaded" \ "lexerSrc" mustBe JsDefined(JsString("lexer grammar test; ID: 'id'+;"))

      contentAsJson(response) \ "parsed" \ "grammar" \ "rules" mustBe JsDefined(JsArray(Seq(JsString("id"))))
    }

    "return 404 for non-existing record" in {
      val response = sendLoadRequest("fedcba")

      status(response) mustBe NOT_FOUND
    }
  }

}
