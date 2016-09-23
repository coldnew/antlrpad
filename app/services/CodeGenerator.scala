package services

import scala.concurrent.Future
import scala.util.Random
import scalaz.\/
import scalaz.Scalaz._

trait CodeValidator {
  def isUnique(code: String): Future[Boolean]
}

class CodeGenerator(length: Int, attempts: Int, validator: CodeValidator) {

  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  def getRandomChar(index: Int): Char = {
    val charCode = Random.nextInt(3) match {
      case 0 => Random.nextInt(10) + '0'
      case 1 => Random.nextInt(26) + 'A'
      case _ => Random.nextInt(26) + 'a'
    }

    charCode.toChar
  }

  def getNewCode: Future[Error \/ String] = {

    def generate(attempt: Int): Future[Error \/ String] = {
      val code = (0 until length).map(getRandomChar).mkString

      if (attempt == 0) Future.successful(Error(s"Cannot generate unique code, limit $attempts reached").left)
      else validator.isUnique(code).flatMap(unique => {
        if (unique) Future.successful(code.right)
        else generate(attempt - 1)
      })
    }

    generate(attempts)
  }

}
