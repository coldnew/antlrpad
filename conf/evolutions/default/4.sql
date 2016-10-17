# --- !Ups


INSERT INTO "ParsedResults" ("grammar", "lexer", "src", "code", "rule")
VALUES (
    '
parser grammar MySQLParser;;

options
   { tokenVocab = MySQLLexer;; }

stat
   : select_clause+
   ;;

schema_name
   : ID
   ;;

select_clause
   : SELECT column_list_clause ( FROM table_references )? ( where_clause )?
   ;;

table_name
   : ID
   ;;

table_alias
   : ID
   ;;

column_name
   : ( ( schema_name DOT )? ID DOT )? ID ( column_name_alias )? | ( table_alias DOT )? ID | USER_VAR ( column_name_alias )?
   ;;

column_name_alias
   : ID
   ;;

index_name
   : ID
   ;;

column_list
   : LPAREN column_name ( COMMA column_name )* RPAREN
   ;;

column_list_clause
   : column_name ( COMMA column_name )*
   ;;

from_clause
   : FROM table_name ( COMMA table_name )*
   ;;

select_key
   : SELECT
   ;;

where_clause
   : WHERE expression
   ;;

expression
   : simple_expression ( expr_op simple_expression )*
   ;;

element
   : USER_VAR | ID | ( ''|'' ID ''|'' ) | INT | column_name
   ;;

right_element
   : element
   ;;

left_element
   : element
   ;;

target_element
   : element
   ;;

relational_op
   : EQ | LTH | GTH | NOT_EQ | LET | GET
   ;;

expr_op
   : AND | XOR | OR | NOT
   ;;

between_op
   : BETWEEN
   ;;

is_or_is_not
   : IS | IS NOT
   ;;

simple_expression
   : left_element relational_op right_element | target_element between_op left_element AND right_element | target_element is_or_is_not NULL
   ;;

table_references
   : table_reference ( ( COMMA table_reference ) | join_clause )*
   ;;

table_reference
   : table_factor1 | table_atom
   ;;

table_factor1
   : table_factor2 ( ( INNER | CROSS )? JOIN table_atom ( join_condition )? )?
   ;;

table_factor2
   : table_factor3 ( STRAIGHT_JOIN table_atom ( ON expression )? )?
   ;;

table_factor3
   : table_factor4 ( ( LEFT | RIGHT ) ( OUTER )? JOIN table_factor4 join_condition )?
   ;;

table_factor4
   : table_atom ( NATURAL ( ( LEFT | RIGHT ) ( OUTER )? )? JOIN table_atom )?
   ;;

table_atom
   : ( table_name ( partition_clause )? ( table_alias )? ( index_hint_list )? ) | ( subquery subquery_alias ) | ( LPAREN table_references RPAREN ) | ( OJ table_reference LEFT OUTER JOIN table_reference ON expression )
   ;;

join_clause
   : ( ( INNER | CROSS )? JOIN table_atom ( join_condition )? ) | ( STRAIGHT_JOIN table_atom ( ON expression )? ) | ( ( LEFT | RIGHT ) ( OUTER )? JOIN table_factor4 join_condition ) | ( NATURAL ( ( LEFT | RIGHT ) ( OUTER )? )? JOIN table_atom )
   ;;

join_condition
   : ( ON expression ( expr_op expression )* ) | ( USING column_list )
   ;;

index_hint_list
   : index_hint ( COMMA index_hint )*
   ;;

index_options
   : ( INDEX | KEY ) ( FOR ( ( JOIN ) | ( ORDER BY ) | ( GROUP BY ) ) )?
   ;;

index_hint
   : USE index_options LPAREN ( index_list )? RPAREN | IGNORE index_options LPAREN index_list RPAREN
   ;;

index_list
   : index_name ( COMMA index_name )*
   ;;

partition_clause
   : PARTITION LPAREN partition_names RPAREN
   ;;

partition_names
   : partition_name ( COMMA partition_name )*
   ;;

partition_name
   : ID
   ;;

subquery_alias
   : ID
   ;;

subquery
   : LPAREN select_clause RPAREN
   ;;',
    '
lexer grammar MySQLLexer;;
@ header {
 }

SELECT
   : ''select''
   ;;


FROM
   : ''from''
   ;;


WHERE
   : ''where''
   ;;


AND
   : ''and'' | ''&&''
   ;;


OR
   : ''or'' | ''||''
   ;;


XOR
   : ''xor''
   ;;


IS
   : ''is''
   ;;


NULL
   : ''null''
   ;;


LIKE
   : ''like''
   ;;


IN
   : ''in''
   ;;


EXISTS
   : ''exists''
   ;;


ALL
   : ''all''
   ;;


ANY
   : ''any''
   ;;


TRUE
   : ''true''
   ;;


FALSE
   : ''false''
   ;;


DIVIDE
   : ''div'' | ''/''
   ;;


MOD
   : ''mod'' | ''%''
   ;;


BETWEEN
   : ''between''
   ;;


REGEXP
   : ''regexp''
   ;;


PLUS
   : ''+''
   ;;


MINUS
   : ''-''
   ;;


NEGATION
   : ''~''
   ;;


VERTBAR
   : ''|''
   ;;


BITAND
   : ''&''
   ;;


POWER_OP
   : ''^''
   ;;


BINARY
   : ''binary''
   ;;


SHIFT_LEFT
   : ''<<''
   ;;


SHIFT_RIGHT
   : ''>>''
   ;;


ESCAPE
   : ''escape''
   ;;


ASTERISK
   : ''*''
   ;;


RPAREN
   : '')''
   ;;


LPAREN
   : ''(''
   ;;


RBRACK
   : '']''
   ;;


LBRACK
   : ''[''
   ;;


COLON
   : '':''
   ;;


ALL_FIELDS
   : ''.*''
   ;;


EQ
   : ''=''
   ;;


LTH
   : ''<''
   ;;


GTH
   : ''>''
   ;;


NOT_EQ
   : ''!=''
   ;;


NOT
   : ''not''
   ;;


LET
   : ''<=''
   ;;


GET
   : ''>=''
   ;;


SEMI
   : '';;''
   ;;


COMMA
   : '',''
   ;;


DOT
   : ''.''
   ;;


COLLATE
   : ''collate''
   ;;


INNER
   : ''inner''
   ;;


OUTER
   : ''outer''
   ;;


JOIN
   : ''join''
   ;;


CROSS
   : ''cross''
   ;;


USING
   : ''using''
   ;;


INDEX
   : ''index''
   ;;


KEY
   : ''key''
   ;;


ORDER
   : ''order''
   ;;


GROUP
   : ''group''
   ;;


BY
   : ''by''
   ;;


FOR
   : ''for''
   ;;


USE
   : ''use''
   ;;


IGNORE
   : ''ignore''
   ;;


PARTITION
   : ''partition''
   ;;


STRAIGHT_JOIN
   : ''straight_join''
   ;;


NATURAL
   : ''natural''
   ;;


LEFT
   : ''left''
   ;;


RIGHT
   : ''right''
   ;;


OJ
   : ''oj''
   ;;


ON
   : ''on''
   ;;


ID
   : ( ''a'' .. ''z'' | ''A'' .. ''Z'' | ''_'' )+
   ;;


INT
   : ''0'' .. ''9''+
   ;;


NEWLINE
   : ''\r''? ''\n'' -> skip
   ;;


WS
   : ( '' '' | ''\t'' | ''\n'' | ''\r'' )+ -> skip
   ;;


USER_VAR
   : ''@'' ( USER_VAR_SUBFIX1 | USER_VAR_SUBFIX2 | USER_VAR_SUBFIX3 | USER_VAR_SUBFIX4 )
   ;;


fragment USER_VAR_SUBFIX1
   : ( ''`'' ( ~ ''`'' )+ ''`'' )
   ;;


fragment USER_VAR_SUBFIX2
   : ( ''\'''' ( ~ ''\'''' )+ ''\'''' )
   ;;


fragment USER_VAR_SUBFIX3
   : ( ''\"'' ( ~ ''\"'' )+ ''\"'' )
   ;;


fragment USER_VAR_SUBFIX4
   : ( ''A'' .. ''Z'' | ''a'' .. ''z'' | ''_'' | ''$'' | ''0'' .. ''9'' | DOT )+
   ;;',
    'select id, name, age from People where age > 30',
    'mysql',
    'stat')

# --- !Downs

delete from "ParsedResults" where "code" = 'mysql'