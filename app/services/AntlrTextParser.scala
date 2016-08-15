package services

import models.ParseTree
import org.antlr.v4.runtime._
import org.antlr.v4.tool.{Grammar, GrammarParserInterpreter, LexerGrammar}

import scala.collection.JavaConverters._
import models.{Failure, Success}

import scalaz.\/
import scalaz.Scalaz._

case class ParseTextSuccess(tree: ParseTree, rule: String, messages: Seq[ParseMessage], parsedGrammar: ParseGrammarSuccess) extends Success
case class ParseTextFailure() extends Failure

class AntlrTextParser {
  def parse(src: String, startRule: String, parsedGrammar: ParseGrammarSuccess): ParseTextFailure \/ ParseTextSuccess = {
    val (tree, rulesNames, rule, errors) = parseText(src, startRule, parsedGrammar.grammar, parsedGrammar.lexerGrammar)
    val treeModel = getTreeModel(tree, rulesNames)

    ParseTextSuccess(treeModel, rule, errors, parsedGrammar).right
  }

  private def parseText(src: String, startRule: String, grammar: Grammar, lexerGrammar: LexerGrammar) = {
    val lexerInterpreter = lexerGrammar.createLexerInterpreter(new ANTLRInputStream(src))
    val tokens = new CommonTokenStream(lexerInterpreter)

    val ruleNames = grammar.getRuleNames()

    val errorListener = new TextParserErrorListener()
    val parser = new GrammarParserInterpreter(grammar, grammar.atn, tokens)
    parser.removeErrorListeners()
    parser.addErrorListener(errorListener)

    val startRuleIndex = Math.max(ruleNames.indexOf(startRule), 0)

    val tree = parser.parse(startRuleIndex)

    (tree, ruleNames, ruleNames(startRuleIndex), errorListener.errors)
  }

  private def getTreeModel(node: ParserRuleContext, ruleNames: Array[String]): ParseTree = {
    val children = node.children.asScala.flatMap { _ match {
        case c: ParserRuleContext => Seq(getTreeModel(c, ruleNames))
        case _ => Seq.empty
      }
    }

    ParseTree(node.getText, ruleNames(node.getRuleIndex), children, node.exception != null)
  }
}
