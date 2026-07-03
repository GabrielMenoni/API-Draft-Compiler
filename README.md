# API-Draft Compiler

## O que é

O API-Draft Compiler transforma descrições de APIs escritas em uma DSL pequena e legível em boilerplate para dois ecossistemas:

- Kotlin com Spring Boot;
- TypeScript com NestJS.

Um arquivo `.apid` declara entidades, seus campos e rotas HTTP. Antes de gerar o código, o compilador executa análises léxica, sintática e semântica e apresenta mensagens com linha e coluna quando encontra construções inválidas.

## Linguagem API-Draft

### Tipos suportados

| API-Draft | Kotlin | TypeScript |
|---|---|---|
| `string` | `String` | `string` |
| `int` | `Int` | `number` |
| `bool` | `Boolean` | `boolean` |
| `float` | `Float` | `number` |
| nome de entidade | `<Entidade>Dto` | `<Entidade>Dto` |
| `List<T>` | `List<T>` | `T[]` |

Listas podem envolver tipos primitivos ou entidades declaradas. Comentários de uma linha começam com `//`.

### Sintaxe completa (com exemplos)

Uma entidade começa com `ENTITY`, recebe um nome e possui um ou mais campos separados por vírgula:

```apid
ENTITY Author {
  string name,
  string email
}

ENTITY Post {
  string title,
  int views,
  bool published,
  Author author,
  List<Author> reviewers
}
```

Uma rota usa um dos métodos `GET`, `POST`, `PUT`, `DELETE` ou `PATCH`, seguido pelo caminho e pelo tipo de retorno:

```apid
ROUTE GET "/posts" RETURNS List<Post>
ROUTE POST "/posts" RETURNS Post
ROUTE PATCH "/posts/published" RETURNS bool
```

O caminho deve começar com `/`. Segmentos estáticos aceitam letras, números, `_` e `-`, e parâmetros podem usar tanto `{id}` quanto `:id`:

```apid
ROUTE GET "/user-profiles/{id}" RETURNS Post
ROUTE DELETE "/user-profiles/:id" RETURNS bool
```

As duas notações de parâmetro são equivalentes. Ao gerar código, o compilador usa `{id}` para Spring e `:id` para NestJS.

Um programa completo pode misturar declarações de entidades e rotas. O exemplo canônico está em [`examples/blog-api.apid`](examples/blog-api.apid):

```apid
ENTITY Author {
  string name,
  string email
}

ENTITY Post {
  string title,
  string content,
  int views,
  bool published,
  Author author
}

ROUTE GET "/authors" RETURNS List<Author>
ROUTE POST "/authors" RETURNS Author
ROUTE GET "/posts" RETURNS List<Post>
ROUTE POST "/posts" RETURNS Post
```

O analisador semântico rejeita entidades e campos duplicados, tipos de campo ou retorno não declarados, caminhos inválidos, rotas duplicadas, colisões de nomes no código gerado e dependências circulares entre entidades. Quando há mais de um erro semântico, todos os erros encontrados são exibidos antes do encerramento.

## Instalação e build

Pré-requisitos:

- JDK 11 ou superior;
- Maven 3.8 ou superior.

Clone o repositório e gere o JAR executável:

```bash
git clone https://github.com/GabrielMenoni/API-Draft-Compiler.git
cd API-Draft-Compiler
mvn clean package
```

O artefato será criado em `target/api-draft-compiler-jar-with-dependencies.jar`.

## Como usar

A CLI recebe o target, o diretório de saída e o arquivo `.apid`, nesta ordem:

```text
java -jar target/api-draft-compiler-jar-with-dependencies.jar \
  --target <kotlin|typescript> --output <diretorio> <arquivo.apid>
```

### Gerar Kotlin/Spring Boot

```bash
java -jar target/api-draft-compiler-jar-with-dependencies.jar \
  --target kotlin --output out/kotlin examples/blog-api.apid
```

Os DTOs são gravados em `out/kotlin/dtos/` e os controllers em `out/kotlin/controllers/`.

### Gerar TypeScript/NestJS

```bash
java -jar target/api-draft-compiler-jar-with-dependencies.jar \
  --target typescript --output out/typescript examples/blog-api.apid
```

Os DTOs são gravados em `out/typescript/dtos/` e os controllers em `out/typescript/controllers/`.

Em uma geração bem-sucedida, o conteúdo anterior das pastas `dtos/` e `controllers/` do diretório informado é substituído, evitando que arquivos gerados por uma execução antiga permaneçam no resultado.

## Executar testes

A suíte compila o projeto, executa os casos léxicos, sintáticos e semânticos, compara recursivamente o código gerado com as saídas esperadas e reporta o total de aprovações:

```bash
./run-tests.sh
```

Também é possível executar uma etapa específica:

```bash
./run-tests.sh lexer
./run-tests.sh parser
./run-tests.sh semantico
./run-tests.sh gerador
./run-tests.sh cli
```

Quando `kotlinc` e/ou `tsc` estão disponíveis no `PATH`, a suíte também compila os arquivos gerados. Caso contrário, ela registra um aviso e continua com as demais verificações.

## Estrutura do projeto

```text
.
├── examples/                         # Programas API-Draft de exemplo
├── src/main/antlr4/                  # Gramáticas do lexer e do parser
├── src/main/java/                    # AST, análises e geradores de código
├── testes/testComp/
│   ├── 1.casos_teste_lexer/
│   ├── 2.casos_teste_parser/
│   └── 3.casos_teste_semantico_gerador/
├── pom.xml
└── run-tests.sh
```

Cada categoria de teste mantém entradas em `entrada/` e resultados esperados em `saida/`. Casos de erro usam arquivos `.txt`; casos de geração preservam a árvore esperada de DTOs e controllers.

## Contribuindo

1. Crie uma branch a partir de `main` com um nome descritivo.
2. Preserve a sintaxe e os padrões de geração já cobertos pela suíte.
3. Adicione casos de entrada e saída esperada para toda alteração de comportamento.
4. Execute `./run-tests.sh` e confirme 100% de aprovação.
5. Abra um pull request explicando a motivação, o comportamento alterado e como ele foi validado.

Não versione arquivos de `target/`, JARs ou saídas locais de geração.
