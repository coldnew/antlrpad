import org.scalatest.TestData
import play.api.db.evolutions.EvolutionsModule
import play.api.db.slick.SlickModule
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Mode}
import repo.{SavedParseResult, ParsedResultsRepository}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

class ParsedResultsRepositoryMock extends ParsedResultsRepository {
  override def insert(parsedResult: SavedParseResult): Future[Int] = Future { 0 }
  override def load(id: Int): Future[Option[SavedParseResult]] = id match {
    case 1 => Future { Some(new SavedParseResult("", "", "", Some(1))) }
    case _ => Future { None }
  } 
  override def save(parsedResult: SavedParseResult): Future[Option[Int]] = Future { Some(1) }
}

trait AntlrFakeApp {
  def antlrFakeApp(testData: TestData): Application = {
    new GuiceApplicationBuilder()
      .in(Mode.Test)
      .disable[SlickModule]
      .disable[EvolutionsModule]
      .disable[play.api.db.slick.evolutions.EvolutionsModule]
      .overrides(bind[ParsedResultsRepository].to[ParsedResultsRepositoryMock])
      .build
  }
}
