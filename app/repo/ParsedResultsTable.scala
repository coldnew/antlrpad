package repo

import javax.inject.{Inject, Singleton}

import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile

import scala.concurrent.Future

case class ParsedResult(grammar: String, src: String, tree: String, id: Option[Int] = None)

@Singleton()
class ParsedResultsRepository @Inject() (protected val dbConfigProvider: DatabaseConfigProvider) extends ParsedResultsTable
    with HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  def insert(parsedResult: ParsedResult): Future[Int] = db.run { resultsTableQueryInc += parsedResult }
  def save(parsedResult: ParsedResult): Future[Option[ParsedResult]] = db.run {
    (resultsTableQuery returning resultsTableQuery).insertOrUpdate(parsedResult)
  }
}

trait ParsedResultsTable {
  self: HasDatabaseConfigProvider[JdbcProfile] =>

  import driver.api._

  class ParsedResultsTable(tag: Tag) extends Table[ParsedResult](tag, "ParsedResults") {
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    val grammar: Rep[String] = column[String]("grammar")
    val src: Rep[String] = column[String]("src")
    val tree: Rep[String] = column[String]("tree")

    def * = (grammar, src, tree, id.?) <> (ParsedResult.tupled, ParsedResult.unapply)
  }

  lazy protected val resultsTableQuery = TableQuery[ParsedResultsTable]
  lazy protected val resultsTableQueryInc = resultsTableQuery returning resultsTableQuery.map(_.id)
}
