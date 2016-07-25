import akka.stream.Materializer
import org.scalatest.TestData
import org.scalatestplus.play._
import play.api.inject._
import play.api.inject.guice._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.test.Helpers._
import play.api.test._
import play.api.{Application, Mode}
import repo.{ParsedResult, ParsedResultsRepository}

import scala.concurrent.Future

class ParsedResultsRepositoryMock extends ParsedResultsRepository {
  override def insert(parsedResult: ParsedResult): Future[Int] = Future { 0 }
  override def load(id: Int): Future[Option[ParsedResult]] = Future { Some(new ParsedResult("", "", "", "", "", None)) }
  override def save(parsedResult: ParsedResult): Future[Option[Int]] = Future { Some(0) }
}

class ApplicationSpec extends PlaySpec with OneAppPerTest {

  implicit lazy val materializer: Materializer = app.materializer

  override def newAppForTest(testData: TestData): Application = {
    new GuiceApplicationBuilder()
      .in(Mode.Test)
      .overrides(bind[ParsedResultsRepository].to[ParsedResultsRepositoryMock])
      .configure(inMemoryDatabase())
      .build
  }

  "Routes" should {

    "send 404 on a bad request" in  {
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

    "give bad request when grammar is incorrect" in {
      val home = route(app, FakeRequest(POST, "/api/parse/").withFormUrlEncodedBody(
        ("grammar", "grammar test; \n two: '2'"),
        ("src", "2"),
        ("rule", "")
      )).get

      status(home) mustBe BAD_REQUEST
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
