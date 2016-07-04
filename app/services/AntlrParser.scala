package services

import models.ParseTreeViewModel
import org.antlr.v4.tool.{Grammar, LexerGrammar}

trait GrammarParser {
  def parseGrammar(src: String): (Grammar, LexerGrammar)
}

trait TextParser {
  def parse(src: String, startRule: String, grammar: Grammar, lexerGrammar: LexerGrammar): ParseTreeViewModel
}

trait Parser {
  this: GrammarParser with TextParser =>

  def parse(grammar: String, startRule: String, src: String): (ParseTreeViewModel, Seq[String])
}

class AntlrParser extends Parser with AntlrGrammarParser with AntlrTextParser {
  def parse(grammar: String, startRule: String, src: String): (ParseTreeViewModel, Seq[String]) = {
    val (g, lg) = parseGrammar(grammar)
    val tree = parse(src, startRule, g, lg)

    (tree, g.getRuleNames)
  }
}
