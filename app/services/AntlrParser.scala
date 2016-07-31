package services

import com.google.inject.Inject
import models.ParseResult

class AntlrParser @Inject() (textParser: AntlrTextParser, grammarParser: AntlrGrammarParser) {

  def parse(grammarSrc: String, startRule: String, src: String): ParseResult = {
    grammarParser.parseGrammar(grammarSrc) match {
      case Right(parsedGrammar) => ParseResult(grammarSrc, src, Some(textParser.parse(src, startRule, parsedGrammar.grammar, parsedGrammar.lexerGrammar)), parsedGrammar.rules, startRule, Seq.empty)
      case Left(f) => ParseResult(grammarSrc, src, None, Seq.empty, startRule, f.errors)
    }
  }
}
