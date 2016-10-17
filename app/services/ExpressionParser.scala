package services

import org.antlr.v4.runtime._
import org.antlr.v4.tool.{Grammar, GrammarParserInterpreter, LexerGrammar}

import scala.collection.JavaConverters._
import scalaz.Scalaz._
import scalaz.\/

class ExpressionParser(parsGrammarResult: ParseGrammarSuccess) {
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

  def getNodeName(node: ParserRuleContext): String = {
    val text = node.getText
    if (text.length > 30) text.substring(0, 30) + "..."
    else text
  }

  private def getTreeModel(node: ParserRuleContext, ruleNames: Array[String]): ParseTree = {
    if (node == null || node.children == null)
      return ParseTree("", "", Seq.empty, false)

    val children = node.children.asScala.flatMap { _ match {
        case c: ParserRuleContext => Seq(getTreeModel(c, ruleNames))
        case _ => Seq.empty
      }
    }

    ParseTree(getNodeName(node), ruleNames(node.getRuleIndex), children, node.exception != null)
  }
}

object ExpressionParser {
  def apply(parsGrammarResult: ParseGrammarSuccess): ExpressionParser = new ExpressionParser(parsGrammarResult)
}