package services

import models.{Failure, ParseTree, Success}
import org.antlr.v4.runtime._
import org.antlr.v4.tool.{Grammar, GrammarParserInterpreter, LexerGrammar}

import scala.collection.JavaConverters._
import scalaz.Scalaz._
import scalaz.\/

case class ParseTextSuccess(tree: ParseTree, rule: String, messages: Seq[ParseMessage], grammar: ParsedGrammar) extends Success
case class ParseTextFailure(error: String) extends Failure

class AntlrTextParser(parsGrammarResult: ParseGrammarSuccess) {
  def parse(src: String, startRule: String): ParseTextFailure \/ ParseTextSuccess = {
    parsGrammarResult match {
      case eg: EmptyGrammar => ParseTextFailure("Cannot parse text with empty grammar provided").left
      case parsedGrammar: ParsedGrammar => {
        val (tree, rulesNames, rule, errors) = parseText(src, startRule, parsedGrammar.grammar, parsedGrammar.lexerGrammar)
        val treeModel = getTreeModel(tree, rulesNames)

        ParseTextSuccess(treeModel, rule, errors, parsedGrammar).right
      }
    }
  }

  private def parseText(src: String, startRule: String, grammar: Grammar, lexerGrammar: LexerGrammar) = {
    val lexerInterpreter = lexerGrammar.createLexerInterpreter(new ANTLRInputStream(src))
    val tokens = new CommonTokenStream(lexerInterpreter)

    val ruleNames = grammar.getRuleNames()

    val errorListener = new TextParserErrorListener()
    val parser = new GrammarParserInterpreter(grammar, grammar.getATN, tokens)
    parser.removeErrorListeners()
    parser.addErrorListener(errorListener)

    val startRuleIndex = Math.max(ruleNames.indexOf(startRule), 0)

    val tree = parser.parse(startRuleIndex)

    (tree, ruleNames, ruleNames(startRuleIndex), errorListener.allMessages)
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
