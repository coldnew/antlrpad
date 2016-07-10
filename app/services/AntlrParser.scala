package services

import com.google.inject.Inject
import models.ParseTreeViewModel

class AntlrParser @Inject() (textParser: AntlrTextParser, grammarParser: AntlrGrammarParser) {

  def parse(grammarSrc: String, startRule: String, src: String): (Option[ParseTreeViewModel], Seq[String]) = {
    grammarParser.parseGrammar(grammarSrc) match {
      case (Some(g), Some(lg)) if lg != null => (Some(textParser.parse(src, startRule, g, lg)), g.getRuleNames)
      case _ => (None, Seq.empty)
    }

  }

}
