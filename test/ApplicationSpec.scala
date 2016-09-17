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

  "Routes" should {

    "send 404 on a non-existing page" in  {
      route(app, FakeRequest(GET, "/boum")).map(status(_)) mustBe Some(NOT_FOUND)
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
      contentAsJson(response) \ "grammar" \ "errors" mustBe JsDefined(Json.parse(MISMATCHED_EOF))
    }

    "return combined grammar warnings" in {
      val response = sendParseRequest(
        ("grammar", "grammar test; \n id: ID; ID: 'a'*;"),
        ("src", "2"),
        ("rule", "")
      )

      status(response) mustBe OK
      contentAsJson(response) \ "grammar" \ "warnings" mustBe JsDefined(Json.parse(RULE_CAN_MATCH_EMPTY_STRING))
    }

    "use lexer grammar when provided" in {
      val response = sendParseRequest(
        ("grammar", "grammar test; \n id: ID; "),
        ("lexer", "lexer grammar test; \n ID: 'a'*;"),
        ("src", "a"),
        ("rule", ""))

      status(response) mustBe OK
      contentAsJson(response) \ "tree" mustBe JsDefined(Json.parse(A_LETTER_PARSE_TREE))
    }

    "return lexer grammar errors" in {
      val response = sendParseRequest(
        ("grammar", "grammar test; \n id: ID; "),
        ("lexer", "lexer grammar test; \n ID: 'a'*"),
        ("src", "a"),
        ("rule", ""))

      status(response) mustBe OK
      contentAsJson(response) \ "grammar" \ "errors" mustBe JsDefined(Json.parse(LEXER_ERROR))
    }

    "return lexer grammar warnings" in {
      val response = sendParseRequest(
        ("grammar", "parser grammar test; \n id: ID; "),
        ("lexer", "lexer grammar test; \n ID: 'a'*;"),
        ("src", "a"),
        ("rule", ""))

      status(response) mustBe OK
      contentAsJson(response) \ "grammar" \ "warnings" mustBe JsDefined(Json.parse(LEXER_WARNING))
    }

    "use implicit lexer for combined grammar" in {
      val response = sendParseRequest(
        ("grammar", "grammar test; \n id: ID; ID: 'a'*;"),
        ("src", "a"),
        ("rule", ""))

      status(response) mustBe OK
      contentAsJson(response) \ "tree" mustBe JsDefined(Json.parse(A_LETTER_PARSE_TREE))
    }
  }

}
