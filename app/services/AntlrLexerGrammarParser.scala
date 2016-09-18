package services

import org.antlr.v4.tool._

class AntlrLexerGrammarParser(useCache: Boolean) extends AntlrBaseGrammarParser(useCache) {
  override val listener: GrammarParserErrorListener = new GrammarParserErrorListener(tool.errMgr, ParseMessage.SourceLexer)
  override def preProcessGrammar(grammar: Grammar): Grammar = grammar
  override def getResult(g: Grammar): ParsedGrammar = ParsedGrammar(null, g.asInstanceOf[LexerGrammar], g.getRuleNames, listener.warnings)
}

