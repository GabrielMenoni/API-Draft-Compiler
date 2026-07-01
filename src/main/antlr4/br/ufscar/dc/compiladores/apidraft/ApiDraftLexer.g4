lexer grammar ApiDraftLexer;

// Keywords
ENTITY  : 'ENTITY' ;
ROUTE   : 'ROUTE' ;
RETURNS : 'RETURNS' ;
GET     : 'GET' ;
POST    : 'POST' ;
PUT     : 'PUT' ;
DELETE  : 'DELETE' ;
PATCH   : 'PATCH' ;
LIST    : 'List' ;

// Primitive types
STRING_TYPE : 'string' ;
INT_TYPE    : 'int' ;
BOOL_TYPE   : 'bool' ;
FLOAT_TYPE  : 'float' ;

// Punctuation
LBRACE : '{' ;
RBRACE : '}' ;
LT     : '<' ;
GT     : '>' ;
COMMA  : ',' ;

// Literals
PATH  : '"' (~["\r\n])* '"' ;
IDENT : [a-zA-Z][a-zA-Z0-9_]* ;

// Ignored
WS      : [ \t\r\n]+ -> skip ;
COMMENT : '//' ~[\r\n]* -> skip ;

// Catch-all for unrecognized characters
ERRO_SIMBOLO : . ;
