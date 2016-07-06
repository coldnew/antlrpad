import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
class ApplicationSpec extends PlaySpec with OneAppPerTest {

  "Routes" should {

    "send 404 on a bad request" in  {
      route(app, FakeRequest(GET, "/boum")).map(status(_)) mustBe Some(NOT_FOUND)
    }

  }

  "ParseController" should {

    "give bad request when no grammar " in {
      val home = route(app, FakeRequest(POST, "/api/parse")).get

      status(home) mustBe BAD_REQUEST
    }

  }

  "ParseController" should {

    "parse the result " in {
      val home = route(app, FakeRequest(POST, "/api/parse").withFormUrlEncodedBody(
        ("grammar", "grammar test; \n two: '2';"),
        ("src", "2"),
        ("rule", "")
      )).get

      status(home) mustBe OK
    }

  }

}
