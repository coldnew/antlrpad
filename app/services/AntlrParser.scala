package services

import models.ParseTreeViewModel
import org.antlr.v4.tool.{Grammar, LexerGrammar}

trait GrammarParser[G, LG] {
  def parseGrammar(src: String): (G, LG)
}

trait TextParser[G, LG, M] {
  def parse(src: String, startRule: String, grammar: G, lexerGrammar: LG): M
}

trait Parser[G, LG, M] {
  this: GrammarParser[G, LG] with TextParser[G, LG, M] =>

  def parse(grammar: String, startRule: String, src: String): (M, Seq[String])
}

class AntlrParser extends Parser[Grammar, LexerGrammar, ParseTreeViewModel] with AntlrGrammarParser with AntlrTextParser {
  def parse(grammar: String, startRule: String, src: String): (ParseTreeViewModel, Seq[String]) = {
    val (g, lg) = parseGrammar(grammar)
    val tree = parse(src, startRule, g, lg)

    (tree, g.getRuleNames)
  }
}
