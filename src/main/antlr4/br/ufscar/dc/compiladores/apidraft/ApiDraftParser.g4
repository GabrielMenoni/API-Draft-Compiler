// Gramática sintática da API-Draft. Um programa é uma sequência de declarações
// de entidade e de rota, em qualquer ordem e quantidade (>= 1); a validação de
// que um tipo referenciado foi de fato declarado antes de ser usado fica a
// cargo do SemanticAnalyzer, não desta gramática.
parser grammar ApiDraftParser;

options { tokenVocab = ApiDraftLexer; }

programa
    : declaracao+ EOF
    ;

declaracao
    : entityDecl
    | routeDecl
    ;

// ENTITY <Nome> { <tipo> <nome>, ... }
entityDecl
    : ENTITY IDENT LBRACE fieldList RBRACE
    ;

fieldList
    : field (COMMA field)*
    ;

field
    : tipo IDENT
    ;

// ROUTE <método> "<caminho>" RETURNS <tipo>
// O caminho chega como um único token PATH (com aspas); validar seu formato
// (deve começar com "/", parâmetros {id} ou :id, etc.) é responsabilidade do
// SemanticAnalyzer via RouteNaming.isValidPath, não desta regra.
routeDecl
    : ROUTE metodoHttp PATH RETURNS tipo
    ;

metodoHttp
    : GET
    | POST
    | PUT
    | DELETE
    | PATCH
    ;

// Tipo primitivo, List<tipo> (recursivo, permite List<List<T>>) ou o nome de
// uma entidade (IDENT) — resolvido contra as entidades declaradas somente na
// análise semântica.
tipo
    : STRING_TYPE
    | INT_TYPE
    | BOOL_TYPE
    | FLOAT_TYPE
    | LIST LT tipo GT
    | IDENT
    ;
