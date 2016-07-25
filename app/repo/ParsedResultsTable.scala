package repo

import javax.inject.{Inject, Singleton}

import com.google.inject.ImplementedBy
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile

import scala.concurrent.Future

case class ParsedResult(grammar: String, src: String, tree: String, rules: String, rule: String, id: Option[Int] = None)

@ImplementedBy(classOf[ParsedResultsRepositoryImpl])
trait ParsedResultsRepository {
  def insert(parsedResult: ParsedResult): Future[Int]
  def save(parsedResult: ParsedResult): Future[Option[Int]]
  def load(id: Int): Future[Option[ParsedResult]]
}

@Singleton()
class ParsedResultsRepositoryImpl @Inject() (protected val dbConfigProvider: DatabaseConfigProvider) extends ParsedResultsTable with ParsedResultsRepository
    with HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  def insert(parsedResult: ParsedResult): Future[Int] = db.run { resultsTableQueryInc += parsedResult }

  def save(parsedResult: ParsedResult): Future[Option[Int]] = db.run {
    resultsTableQueryInc.insertOrUpdate(parsedResult)
  }

  def load(id: Int): Future[Option[ParsedResult]] = db.run {
    resultsTableQuery.filter(_.id === id).result.headOption
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
    val rules: Rep[String] = column[String]("rules")
    val rule: Rep[String] = column[String]("rule")

    def * = (grammar, src, tree, rules, rule, id.?) <> (ParsedResult.tupled, ParsedResult.unapply)
  }

  lazy protected val resultsTableQuery = TableQuery[ParsedResultsTable]
  lazy protected val resultsTableQueryInc = resultsTableQuery returning resultsTableQuery.map(_.id)
}
