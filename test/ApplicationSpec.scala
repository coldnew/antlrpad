import akka.stream.Materializer
import org.scalatest.TestData
import org.scalatestplus.play._
import play.api.Application
import play.api.libs.json._
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test._

import scala.concurrent.Future

class ApplicationSpec extends PlaySpec with OneAppPerTest with AntlrFakeApp {

  implicit lazy val materializer: Materializer = app.materializer

  override def newAppForTest(testData: TestData): Application = antlrFakeApp(testData)

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

    def parseRequest(data: (String, String)*): Future[Result] = {
      route(app, FakeRequest(POST, "/api/parse/").withFormUrlEncodedBody(data: _*)).get
    }

    "give bad request when no grammar " in {
      val home = route(app, FakeRequest(POST, "/api/parse/")).get
      status(home) mustBe BAD_REQUEST
    }

    "return combined grammar errors" in {
      val response = parseRequest(
        ("grammar", "grammar test; \n two: '2'"),
        ("src", "2"),
        ("rule", ""))

      status(response) mustBe OK
      contentAsJson(response) \ "errors" must be (JsDefined(Json.parse("""[{"message":"error(50): :2:9: syntax error: mismatched input '<EOF>' expecting SEMI while matching a rule","errType":"error","col":9,"line":2}]""")))
    }

    "return grammar warnings" in {
      val response = parseRequest(
        ("grammar", "grammar test; \n id: ID; ID: 'a'*;"),
        ("src", "2"),
        ("rule", "")
      )

      status(response) mustBe OK
      contentAsJson(response) \ "parsedGrammar" \ "warnings" must be (JsDefined(Json.parse("""[{"message":"warning(146): :2:9: non-fragment lexer rule ID can match the empty string","errType":"warning","col":9,"line":2}]""")))
    }

    "use lexer grammar when provided" in {

    }

    "return lexer grammar errors" in {

    }

    "return lexer grammar warnings" in {

    }

    "return error when lexer provided for combined grammar" in {

    }

    "use implicit lexer for combined grammar" in {
      val response = parseRequest(
        ("grammar", "grammar test; \n two: '2';"),
        ("src", "2"),
        ("rule", "")
      )

      status(response) mustBe OK
    }
  }

}
