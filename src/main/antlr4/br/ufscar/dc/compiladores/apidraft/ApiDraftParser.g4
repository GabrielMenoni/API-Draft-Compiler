parser grammar ApiDraftParser;

options { tokenVocab = ApiDraftLexer; }

programa
    : declaracao+ EOF
    ;

declaracao
    : entityDecl
    | routeDecl
    ;

entityDecl
    : ENTITY IDENT LBRACE fieldList RBRACE
    ;

fieldList
    : field (COMMA field)*
    ;

field
    : tipo IDENT
    ;

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

tipo
    : STRING_TYPE
    | INT_TYPE
    | BOOL_TYPE
    | FLOAT_TYPE
    | LIST LT tipo GT
    | IDENT
    ;
