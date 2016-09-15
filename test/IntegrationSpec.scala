import org.scalatest.{Ignore, TestData}
import org.scalatestplus.play._
import play.api.Application

@Ignore
class IntegrationSpec extends PlaySpec with OneServerPerTest with OneBrowserPerTest with HtmlUnitFactory with AntlrFakeApp {

  override def newAppForTest(testData: TestData): Application = antlrFakeApp(testData)

  "Application" should {

    "work from within a browser" in {

      go to ("http://localhost:" + port)

      pageSource must include ("AntlrPad")
    }
  }
}
