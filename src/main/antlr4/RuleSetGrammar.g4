grammar RuleSetGrammar;

AND : 'and' | 'AND' ;
OR  : 'or' ;
 
TRUE  : 'true' ;
FALSE : 'false' ;
 
MULT  : '*' ;
DIV   : '/' ;
PLUS  : '+' ;
MINUS : '-' ;
 
GT  : '>' ;
GE  : '>=' ;
LT  : '<' ;
LE  : '<=' ;
EQ  : '=' | '==' ;
NE  : '<>' | '!=' ;
 
LPAREN : '(' ;
RPAREN : ')' ;
 
// DECIMAL, IDENTIFIER, COMMENTS, WS are set using regular expressions
 
DECIMAL : '-'?[0-9]+('.'[0-9]+)? ;
 
IDENTIFIER : [a-zA-Z_][a-zA-Z_0-9]* ;
 
SEMI : ';' ;
 
// COMMENT and WS are stripped from the output token stream by sending
// to a different channel 'skip'
 
COMMENT : '//' .+? ('\n'|EOF) -> skip ;
 
WS : [ \r\t\u000C\n]+ -> skip ;

fragment ESCAPED_QUOTE : '\\"';
QUOTED_STRING :   '"' ( ESCAPED_QUOTE | ~('\n'|'\r') )*? '"';



rule_set : single_rule* ;

single_rule : rule_identifier rule_predicate rule_prediction train_test_measures?;

rule_identifier : IDENTIFIER ':' ;
rule_predicate : 'PREDICATE' ':'  logical_expr ;

rule_prediction : 'PREDICTION' ':' logical_entity 
                | 'PREDICTION' ':' numeric_entity;

train_test_measures : 'Training/test measures' ':'
             (  'recordCount'     DECIMAL )?
             (  'nbCorrect'       DECIMAL )?
             (  'confidence'      DECIMAL )?
             (  'weight'          DECIMAL )?
  ;
 
condition : logical_expr ;
conclusion : IDENTIFIER ;
 
logical_expr
 : logical_expr (AND logical_expr)+ # LogicalExpressionAnd
 | logical_expr OR logical_expr  # LogicalExpressionOr
 | comparison_expr               # ComparisonExpression
 | LPAREN logical_expr RPAREN    # LogicalExpressionInParen
 | logical_entity                # LogicalEntity
 ;
 
comparison_expr : comparison_operand comp_operator comparison_operand # ComparisonExpressionWithOperator
                | LPAREN comparison_expr RPAREN # ComparisonExpressionParens
                ;
 
comparison_operand : arithmetic_expr  # ComparisonOperandExpr
                   | QUOTED_STRING    # ComparisonOperandString
                   ;
 
comp_operator : EQ # OperatorEqual
              | NE # OperatorNotEqual
              | LT # OperatorLessThan
              | LE # OperatorLessOrEqual
              | GT # OperatorGreaterThan
              | GE # OperatorGreaterOrEqual
              ;

arithmetic_expr
 : arithmetic_expr MULT arithmetic_expr  # ArithmeticExpressionMult
 | arithmetic_expr DIV arithmetic_expr   # ArithmeticExpressionDiv
 | arithmetic_expr PLUS arithmetic_expr  # ArithmeticExpressionPlus
 | arithmetic_expr MINUS arithmetic_expr # ArithmeticExpressionMinus
 | MINUS arithmetic_expr                 # ArithmeticExpressionNegation
 | LPAREN arithmetic_expr RPAREN         # ArithmeticExpressionParens
 | numeric_entity                        # ArithmeticExpressionNumericEntity
 ;
 
logical_entity : (TRUE | FALSE) # LogicalConst
               | IDENTIFIER     # LogicalVariable
               | QUOTED_STRING  # LogicalString
               ;
 
numeric_entity : DECIMAL              # NumericConst
               | IDENTIFIER           # NumericVariable
               ;
               
