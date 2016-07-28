package services

import models.ParseTree
import org.antlr.v4.runtime.{ANTLRInputStream, CommonTokenStream, ParserRuleContext}
import org.antlr.v4.tool.{Grammar, GrammarParserInterpreter, LexerGrammar}

import scala.collection.JavaConverters._

class AntlrTextParser {
  def parse(src: String, startRule: String, grammar: Grammar, lexerGrammar: LexerGrammar): ParseTree = {
    val (tree, rulesNames) = parseText(src, startRule, grammar, lexerGrammar)
    getTreeModel(tree, rulesNames)
  }

  private def parseText(src: String, startRule: String, grammar: Grammar, lexerGrammar: LexerGrammar): (ParserRuleContext, Array[String]) = {
    val lexerInterpreter = lexerGrammar.createLexerInterpreter(new ANTLRInputStream(src))
    val tokens = new CommonTokenStream(lexerInterpreter)

    val ruleNames = grammar.getRuleNames()

    val parser = new GrammarParserInterpreter(grammar, grammar.atn, tokens)
    val startRuleIndex = Math.max(ruleNames.indexOf(startRule), 0)

    val tree = parser.parse(startRuleIndex)

    (tree, ruleNames)
  }

  private def getTreeModel(node: ParserRuleContext, ruleNames: Array[String]): ParseTree = {
    val children = node.children.asScala.flatMap { _ match {
        case c: ParserRuleContext => Seq(getTreeModel(c, ruleNames))
        case _ => Seq.empty
      }
    }

    new ParseTree(node.getText, ruleNames(node.getRuleIndex), children)
  }
}
