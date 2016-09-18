package services

import org.antlr.v4.tool.Grammar

class GrammarParser(useCache: Boolean, lexer: ParseGrammarSuccess) extends BaseGrammarParser(useCache) {
  override val listener: GrammarParserErrorListener = new GrammarParserErrorListener(tool.errMgr, ParseMessage.SourceParser)
  override def preProcessGrammar(grammar: Grammar): Grammar = {
    lexer match {
      case eg: EmptyGrammar => grammar
      case lg: ParsedGrammar => {
        grammar.importVocab(lg.lexerGrammar)
        grammar
      }
    }
  }
  override def getResult(g: Grammar): ParsedGrammar = {
    lexer match {
      case _: EmptyGrammar => ParsedGrammar(g, g.getImplicitLexer, g.getRuleNames, listener.warnings)
      case lg: ParsedGrammar => ParsedGrammar(g, lg.lexerGrammar, g.getRuleNames, listener.warnings ++ lg.warnings)
    }
  }
}

object GrammarParser {
  def apply(useCache: Boolean, lexer: ParseGrammarSuccess): GrammarParser = new GrammarParser(useCache, lexer)
}