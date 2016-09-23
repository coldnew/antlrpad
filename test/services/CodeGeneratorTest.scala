package services

import org.scalatest.MustMatchers
import org.scalatest.concurrent.{Futures, ScalaFutures}
import org.scalatestplus.play.PlaySpec

import scala.concurrent.Future
import scalaz.{-\/, \/-}

class CodeGeneratorTest extends PlaySpec with MustMatchers with ScalaFutures {

  class MockValidator(unique: Boolean) extends CodeValidator {
    override def isUnique(code: String): Future[Boolean] = {
      Future.successful(unique)
    }
  }

  "Code generator" should {
    "return valid code" in {
      val generator = new CodeGenerator(6, 1, new MockValidator(true))
      val code = generator.getNewCode

      whenReady(code) { r =>
        r mustBe a [\/-[_]]
        r map { c =>
          c must not be empty
          c.length mustBe 6
          c must fullyMatch regex "[a-zA-Z0-9]+"
        }
      }
    }

    "return error when limit is reached" in {
      val generator = new CodeGenerator(6, 1, new MockValidator(false))
      val code = generator.getNewCode

      whenReady(code) { r =>
        r mustBe a [-\/[_]]
        r leftMap { err =>
          err.msg mustBe "Cannot generate unique code, limit 1 reached"
        }
      }
    }
  }

}
