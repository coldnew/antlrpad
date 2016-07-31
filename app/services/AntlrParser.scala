package services

import com.google.inject.Inject

class AntlrParser @Inject() (textParser: AntlrTextParser, grammarParser: AntlrGrammarParser) {

//  def parse(grammarSrc: String, startRule: String, src: String): ParseSuccess = {
//    grammarParser.parseGrammar(grammarSrc) match {
//      case (Some(g), Some(lg)) => ParseSuccess(grammarSrc, src, Some(textParser.parse(src, startRule, g, lg)), g.getRuleNames, startRule)
//      case _ => ParseSuccess(grammarSrc, src, None, Seq.empty, startRule)
//    }
//  }
}
