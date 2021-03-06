package repo

import javax.inject.{Inject, Singleton}

import com.google.inject.ImplementedBy
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import services.CodeValidator
import slick.driver.JdbcProfile

import scala.concurrent.Future

case class SavedParseResult(grammar: String, lexer: String, rule: String, src: String, code: String, id: Option[Int] = None)

@ImplementedBy(classOf[ParsedResultsRepositoryImpl])
trait ParsedResultsRepository {
  def save(parsedResult: SavedParseResult): Future[String]
  def load(id: String): Future[Option[SavedParseResult]]
  def codeUnique(code: String): Future[Boolean]
}

@Singleton()
class ParsedResultsRepositoryImpl @Inject() (protected val dbConfigProvider: DatabaseConfigProvider) extends ParsedResultsTable with ParsedResultsRepository
    with HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._
  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  def saveAndGetCode(parsedResult: SavedParseResult): Future[Option[Int]] = db.run {
    resultsTableQueryInc.insertOrUpdate(parsedResult)
  }

  def load(id: String): Future[Option[SavedParseResult]] = db.run {
    resultsTableQuery.filter(_.code === id).result.headOption
  }

  def codeUnique(code: String): Future[Boolean] = db.run {
    resultsTableQuery.filter(_.code === code).length.result.map(_ == 0)
  }

  override def save(parsedResult: SavedParseResult): Future[String] = saveAndGetCode(parsedResult).map(x => parsedResult.code)
}

class DbCodeValidator(parsedResultsRepository: ParsedResultsRepository) extends CodeValidator {
  override def isUnique(code: String): Future[Boolean] = {
    parsedResultsRepository.codeUnique(code)
  }
}

trait ParsedResultsTable {
  self: HasDatabaseConfigProvider[JdbcProfile] =>

  import driver.api._

  class ParsedResultsTable(tag: Tag) extends Table[SavedParseResult](tag, "ParsedResults") {
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    val grammar: Rep[String]  = column[String]("grammar")
    val lexer: Rep[String]    = column[String]("lexer")
    val rule: Rep[String]     = column[String]("rule")
    val src: Rep[String]      = column[String]("src")
    val code: Rep[String]     = column[String]("code")

    def * = (grammar, lexer, rule, src, code, id.?) <> (SavedParseResult.tupled, SavedParseResult.unapply)
  }

  lazy protected val resultsTableQuery = TableQuery[ParsedResultsTable]
  lazy protected val resultsTableQueryInc = resultsTableQuery returning resultsTableQuery.map(_.id)
}
