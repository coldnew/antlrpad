package services

import org.antlr.v4.tool.{Grammar, LexerGrammar}

trait AntlrGrammarParser extends GrammarParser[Grammar, LexerGrammar] {
  def parseGrammar(src: String): (Option[Grammar], Option[LexerGrammar]) = {
    val tool = new org.antlr.v4.Tool()
    val grammarRootAst = tool.parseGrammarFromString(src)

    val grammar = Some(tool.createGrammar(grammarRootAst))
    tool.process(grammar.get, false)  // safe to unwrap here, null is handled in process. Doesn't look good though

    (grammar, grammar map { _.getImplicitLexer })
  }
}