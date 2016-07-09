package services

import models.ParseTreeViewModel
import org.antlr.v4.tool.{Grammar, LexerGrammar}

trait GrammarParser[G, LG] {
  def parseGrammar(src: String): (Option[G], Option[LG])
}

trait TextParser[G, LG, M] {
  def parse(src: String, startRule: String, grammar: G, lexerGrammar: LG): M
}

trait Parser[G, LG, M] {
  this: GrammarParser[G, LG] with TextParser[G, LG, M] =>

  def parse(grammar: String, startRule: String, src: String): (Option[M], Seq[String])
}

class AntlrParser extends Parser[Grammar, LexerGrammar, ParseTreeViewModel] with AntlrGrammarParser with AntlrTextParser {
  def parse(grammar: String, startRule: String, src: String): (Option[ParseTreeViewModel], Seq[String]) = {

    parseGrammar(grammar) match {
      case (Some(g), Some(lg)) => {
        val tree = parse(src, startRule, g, lg)
        (Some(tree), g.getRuleNames)
      }
      case (_, _) => (None, Seq.empty)
    }

  }
}
