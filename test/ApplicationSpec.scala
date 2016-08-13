import akka.stream.Materializer
import org.scalatest.TestData
import org.scalatestplus.play._
import play.api.Application
import play.api.test.Helpers._
import play.api.test._

class ApplicationSpec extends PlaySpec with OneAppPerTest with AntlrFakeApp {

  implicit lazy val materializer: Materializer = app.materializer

  override def newAppForTest(testData: TestData): Application = antlrFakeApp(testData)

  "Routes" should {

    "send 404 on a non-existing paget" in  {
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

    "return grammar errors" in {
      val home = route(app, FakeRequest(POST, "/api/parse/").withFormUrlEncodedBody(
        ("grammar", "grammar test; \n two: '2'"),
        ("src", "2"),
        ("rule", "")
      )).get

      status(home) mustBe OK
      contentAsString(home) must include("syntax error: mismatched input")
    }

    "return grammar warnings" in {
      val req = route(app, FakeRequest(POST, "/api/parse/").withFormUrlEncodedBody(
        ("grammar", "grammar test; \n id: ID; ID: 'a'*;"),
        ("src", "2"),
        ("rule", "")
      )).get

      status(req) mustBe OK
      contentAsString(req) must include("non-fragment lexer rule ID can match the empty string")
    }

    "parse the result " in {
      val home = route(app, FakeRequest(POST, "/api/parse/").withFormUrlEncodedBody(
        ("grammar", "grammar test; \n two: '2';"),
        ("src", "2"),
        ("rule", "")
      )).get

      status(home) mustBe OK
    }
  }

}
