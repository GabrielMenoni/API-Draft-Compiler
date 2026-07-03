// Lexer da linguagem API-Draft. Palavras-chave e pontuação são poucas de
// propósito: a linguagem é declarativa (só ENTITY e ROUTE), então o léxico
// fica pequeno e a complexidade toda mora no parser e na análise semântica.
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
PATH          : '"' (~["\r\n])* '"' ;
// Casa uma aspa de abertura sem a de fechamento antes da quebra de linha ou do
// fim do arquivo. Como o ANTLR tenta as regras na ordem declarada e escolhe o
// match mais longo, PATH sempre vence quando o caminho está bem formado; esta
// regra só é usada para dar um erro específico ("caminho não fechado") em vez
// de um erro sintático genérico apontando para o restante da linha.
UNCLOSED_PATH : '"' (~["\r\n])* ;
IDENT : [a-zA-Z][a-zA-Z0-9_]* ;

// Ignored
WS      : [ \t\r\n]+ -> skip ;
COMMENT : '//' ~[\r\n]* -> skip ;

// Casa qualquer caractere não reconhecido pelas regras acima. Sem isso, o ANTLR
// pararia a análise léxica no primeiro símbolo inválido; com o catch-all, o
// caractere vira um token ERRO_SIMBOLO que CustomErrorListener transforma numa
// mensagem de erro léxico, permitindo continuar e reportar outros problemas.
ERRO_SIMBOLO : . ;
