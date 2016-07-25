import org.scalatest.TestData
import org.scalatestplus.play._
import play.api.Application

/**
 * add your integration spec here.
 * An integration test will fire up a whole play application in a real (or headless) browser
 */
class IntegrationSpec extends PlaySpec with OneServerPerTest with OneBrowserPerTest with HtmlUnitFactory with AntlrFakeApp {

  override def newAppForTest(testData: TestData): Application = antlrFakeApp(testData)

  "Application" should {

    "work from within a browser" in {

      go to ("http://localhost:" + port)

      pageSource must include ("AntlrPad")
    }
  }
}
