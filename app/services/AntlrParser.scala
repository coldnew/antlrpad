package services

import com.google.inject.Inject
import models.ParseResult

class AntlrParser @Inject() (textParser: AntlrTextParser, grammarParser: AntlrGrammarParser) {

  def parse(grammarSrc: String, startRule: String, src: String): ParseResult = {
    grammarParser.parseGrammar(grammarSrc) match {
      case (Some(g), Some(lg)) => ParseResult(grammarSrc, src, Some(textParser.parse(src, startRule, g, lg)), g.getRuleNames, startRule)
      case _ => ParseResult(grammarSrc, src, None, Seq.empty, startRule)
    }
  }
}
