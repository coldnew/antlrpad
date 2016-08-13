package repo

import javax.inject.{Inject, Singleton}

import com.google.inject.ImplementedBy
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile

import scala.concurrent.Future

case class SavedParseResult(grammar: String, src: String, id: Option[Int] = None)

@ImplementedBy(classOf[ParsedResultsRepositoryImpl])
trait ParsedResultsRepository {
  def insert(parsedResult: SavedParseResult): Future[Int]
  def save(parsedResult: SavedParseResult): Future[Option[Int]]
  def load(id: Int): Future[Option[SavedParseResult]]
}

@Singleton()
class ParsedResultsRepositoryImpl @Inject() (protected val dbConfigProvider: DatabaseConfigProvider) extends ParsedResultsTable with ParsedResultsRepository
    with HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  def insert(parsedResult: SavedParseResult): Future[Int] = db.run { resultsTableQueryInc += parsedResult }

  def save(parsedResult: SavedParseResult): Future[Option[Int]] = db.run {
    resultsTableQueryInc.insertOrUpdate(parsedResult)
  }

  def load(id: Int): Future[Option[SavedParseResult]] = db.run {
    resultsTableQuery.filter(_.id === id).result.headOption
  }
}

trait ParsedResultsTable {
  self: HasDatabaseConfigProvider[JdbcProfile] =>

  import driver.api._

  class ParsedResultsTable(tag: Tag) extends Table[SavedParseResult](tag, "ParsedResults") {
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    val grammar: Rep[String] = column[String]("grammar")
    val src: Rep[String] = column[String]("src")

    def * = (grammar, src, id.?) <> (SavedParseResult.tupled, SavedParseResult.unapply)
  }

  lazy protected val resultsTableQuery = TableQuery[ParsedResultsTable]
  lazy protected val resultsTableQueryInc = resultsTableQuery returning resultsTableQuery.map(_.id)
}
