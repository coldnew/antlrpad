package services

import org.antlr.v4.tool.Grammar
import utils.InMemoryCache

import scalaz.\/
import scalaz.Scalaz._

import utils.Cached._

abstract class BaseGrammarParser(useCache: Boolean) {

  implicit lazy val inMemoryCache = new InMemoryCache[Int, ParseGrammarFailure \/ ParseGrammarSuccess]()

  protected val tool = new org.antlr.v4.Tool()

  val listener: GrammarParserErrorListener
  def preProcessGrammar(grammar: Grammar): Grammar
  def getResult(g: Grammar): ParsedGrammar

  def parse(src: String): ParseGrammarFailure \/ ParseGrammarSuccess = {
    if (src == null || src.isEmpty) {
      EmptyGrammar().right
    }
    else {
      cache(useCache) by src.hashCode value {
        tool.removeListeners()
        tool.addListener(listener)

        val grammarRootAst = tool.parseGrammarFromString(src)
        val grammar = preProcessGrammar(tool.createGrammar(grammarRootAst))

        tool.process(grammar, false)

        if (listener.errors.isEmpty)
          getResult(grammar).right
        else
          ParseGrammarFailure(listener.errors).left
      }
    }
  }
}
