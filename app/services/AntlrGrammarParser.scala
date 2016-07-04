package services

import org.antlr.v4.tool.{Grammar, LexerGrammar}

trait AntlrGrammarParser extends GrammarParser[Grammar, LexerGrammar] {
  def parseGrammar(src: String): (Grammar, LexerGrammar) = {
    val tool = new org.antlr.v4.Tool()
    val grammarRootAst = tool.parseGrammarFromString(src)
    val grammar = tool.createGrammar(grammarRootAst)

    tool.process(grammar, false)

    (grammar, grammar.implicitLexer)
  }
}