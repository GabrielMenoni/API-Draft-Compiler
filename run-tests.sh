#!/usr/bin/env bash

# Suíte de aceitação do compilador API-Draft.
# Uso: ./run-tests.sh [lexer|parser|semantico|gerador|cli]

set -u

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TESTES_DIR="$SCRIPT_DIR/testes/testComp"
JAR_FILE="$SCRIPT_DIR/target/api-draft-compiler-jar-with-dependencies.jar"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

TOTAL_PASSED=0
TOTAL_FAILED=0
KOTLIN_WARNING_SHOWN=0
TYPESCRIPT_WARNING_SHOWN=0
TMP_ROOT=""
RUN_STATUS=0

print_header() {
    echo -e "${YELLOW}========================================${NC}"
    echo -e "${YELLOW}$1${NC}"
    echo -e "${YELLOW}========================================${NC}"
}

configure_java() {
    if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
        export PATH="$JAVA_HOME/bin:$PATH"
        return 0
    fi

    if command -v java >/dev/null 2>&1; then
        local java_bin
        java_bin="$(readlink -f "$(command -v java)")"
        JAVA_HOME="$(dirname "$(dirname "$java_bin")")"
        export JAVA_HOME
        export PATH="$JAVA_HOME/bin:$PATH"
        return 0
    fi

    local candidate
    for candidate in /usr/lib/jvm/*; do
        if [ -x "$candidate/bin/java" ]; then
            JAVA_HOME="$candidate"
            export JAVA_HOME
            export PATH="$JAVA_HOME/bin:$PATH"
            return 0
        fi
    done

    echo -e "${RED}Java não encontrado. Instale um JDK 11 ou superior.${NC}"
    return 1
}

compile_project() {
    print_header "Compilando projeto"
    if ! configure_java; then
        return 1
    fi
    if ! command -v mvn >/dev/null 2>&1; then
        echo -e "${RED}Maven não encontrado no PATH.${NC}"
        return 1
    fi
    if ! (cd "$SCRIPT_DIR" && mvn clean package -q -DskipTests); then
        echo -e "${RED}Erro ao compilar o projeto.${NC}"
        return 1
    fi
    echo -e "${GREEN}✓ Projeto compilado com sucesso${NC}"
}

pass_test() {
    echo -e "${GREEN}✓ PASS${NC}: $1"
    TOTAL_PASSED=$((TOTAL_PASSED + 1))
}

fail_test() {
    echo -e "${RED}✗ FAIL${NC}: $1"
    TOTAL_FAILED=$((TOTAL_FAILED + 1))
}

run_compiler() {
    local target="$1"
    local input="$2"
    local output="$3"
    local log="$4"

    rm -rf "$output"
    mkdir -p "$output"
    java -jar "$JAR_FILE" --target "$target" --output "$output" "$input" >"$log" 2>&1
    RUN_STATUS=$?
}

show_log() {
    local log="$1"
    if [ -s "$log" ]; then
        sed -n '1,12p' "$log" | sed 's/^/    /'
    else
        echo "    (sem saída)"
    fi
}

run_success_case() {
    local id="$1"
    local category="$2"
    local input="$3"
    local target="${4:-kotlin}"
    local work="$TMP_ROOT/$id"
    local log="$work.log"

    run_compiler "$target" "$input" "$work" "$log"
    if [ "$RUN_STATUS" -eq 0 ] && [ ! -s "$log" ]; then
        pass_test "$id — $category/$(basename "$input")"
    else
        fail_test "$id — $category/$(basename "$input")"
        echo "  Esperado: compilação bem-sucedida e sem mensagens"
        echo "  Obtido (status $RUN_STATUS):"
        show_log "$log"
    fi
}

run_error_case() {
    local id="$1"
    local category="$2"
    local input="$3"
    local expected="$4"
    local work="$TMP_ROOT/$id"
    local log="$work.log"

    run_compiler kotlin "$input" "$work" "$log"
    if [ "$RUN_STATUS" -ne 0 ] && diff -u "$expected" "$log" >/dev/null 2>&1; then
        pass_test "$id — $category/$(basename "$input")"
    else
        fail_test "$id — $category/$(basename "$input")"
        echo "  Status esperado: diferente de zero; obtido: $RUN_STATUS"
        diff -u "$expected" "$log" | sed -n '1,30p' | sed 's/^/  /'
    fi
}

compile_kotlin_output() {
    local generated="$1"
    local case_id="$2"

    if ! command -v kotlinc >/dev/null 2>&1; then
        if [ "$KOTLIN_WARNING_SHOWN" -eq 0 ]; then
            echo -e "${YELLOW}⚠ kotlinc não encontrado; verificação de compilação Kotlin ignorada.${NC}"
            KOTLIN_WARNING_SHOWN=1
        fi
        return 0
    fi

    local verify_dir="$TMP_ROOT/${case_id}-kotlinc"
    local stub="$verify_dir/SpringAnnotations.kt"
    local log="$verify_dir/kotlinc.log"
    mkdir -p "$verify_dir"
    cat >"$stub" <<'KOTLIN_STUB'
package org.springframework.web.bind.annotation

annotation class RestController
annotation class RequestMapping(val value: String = "")
annotation class GetMapping(val value: String = "")
annotation class PostMapping(val value: String = "")
annotation class PutMapping(val value: String = "")
annotation class DeleteMapping(val value: String = "")
annotation class PatchMapping(val value: String = "")
KOTLIN_STUB

    local sources=()
    while IFS= read -r source; do
        sources+=("$source")
    done < <(find "$generated" -type f -name '*.kt' | sort)

    if kotlinc "${sources[@]}" "$stub" -d "$verify_dir/generated.jar" >"$log" 2>&1; then
        return 0
    fi

    echo "  O código Kotlin gerado não compilou:"
    show_log "$log"
    return 1
}

compile_typescript_output() {
    local generated="$1"
    local case_id="$2"

    if ! command -v tsc >/dev/null 2>&1; then
        if [ "$TYPESCRIPT_WARNING_SHOWN" -eq 0 ]; then
            echo -e "${YELLOW}⚠ tsc não encontrado; verificação de compilação TypeScript ignorada.${NC}"
            TYPESCRIPT_WARNING_SHOWN=1
        fi
        return 0
    fi

    local verify_dir="$TMP_ROOT/${case_id}-tsc"
    local stub="$verify_dir/nestjs-common.d.ts"
    local log="$verify_dir/tsc.log"
    mkdir -p "$verify_dir"
    cat >"$stub" <<'TYPESCRIPT_STUB'
declare module '@nestjs/common' {
  export function Controller(path?: string): ClassDecorator;
  export function Get(path?: string): MethodDecorator;
  export function Post(path?: string): MethodDecorator;
  export function Put(path?: string): MethodDecorator;
  export function Delete(path?: string): MethodDecorator;
  export function Patch(path?: string): MethodDecorator;
}
TYPESCRIPT_STUB

    local sources=()
    while IFS= read -r source; do
        sources+=("$source")
    done < <(find "$generated" -type f -name '*.ts' | sort)

    if tsc --noEmit --strict --experimentalDecorators --skipLibCheck \
        --target ES2020 --module ESNext --moduleResolution bundler \
        "${sources[@]}" "$stub" >"$log" 2>&1; then
        return 0
    fi

    echo "  O código TypeScript gerado não compilou:"
    show_log "$log"
    return 1
}

run_generation_case() {
    local id="$1"
    local input="$2"
    local target="$3"
    local expected="$4"
    local work="$TMP_ROOT/${id}-${target}"
    local log="$work.log"
    local diff_log="$TMP_ROOT/${id}-${target}.diff"

    run_compiler "$target" "$input" "$work" "$log"
    if [ "$RUN_STATUS" -ne 0 ] || [ -s "$log" ]; then
        fail_test "$id — geração $target/$(basename "$input")"
        echo "  Compilador encerrou com status $RUN_STATUS:"
        show_log "$log"
        return
    fi

    if ! diff -ru "$expected" "$work" >"$diff_log" 2>&1; then
        fail_test "$id — geração $target/$(basename "$input")"
        echo "  Arquivos gerados divergem da saída esperada:"
        sed -n '1,40p' "$diff_log" | sed 's/^/    /'
        return
    fi

    if [ "$target" = "kotlin" ]; then
        if ! compile_kotlin_output "$work" "$id"; then
            fail_test "$id — geração $target/$(basename "$input")"
            return
        fi
    elif ! compile_typescript_output "$work" "$id"; then
        fail_test "$id — geração $target/$(basename "$input")"
        return
    fi

    pass_test "$id — geração $target/$(basename "$input")"
}

run_lexer_tests() {
    print_header "Categoria 1 — Análise léxica"
    local base="$TESTES_DIR/1.casos_teste_lexer"
    run_success_case L1 lexer "$base/entrada/valido_simples.apid"
    run_error_case L2 lexer "$base/entrada/erro_simbolo.apid" "$base/saida/erro_simbolo.txt"
    run_error_case L3 lexer "$base/entrada/caminho_nao_fechado.apid" "$base/saida/caminho_nao_fechado.txt"
}

run_parser_tests() {
    print_header "Categoria 1 — Análise sintática"
    local base="$TESTES_DIR/2.casos_teste_parser"
    run_success_case P1 parser "$base/entrada/valido_completo.apid"
    run_error_case P2 parser "$base/entrada/erro_chave_faltando.apid" "$base/saida/erro_chave_faltando.txt"
    run_error_case P3 parser "$base/entrada/erro_returns_faltando.apid" "$base/saida/erro_returns_faltando.txt"
    run_success_case P4 parser "$base/entrada/comentarios.apid"
    run_error_case P5 parser "$base/entrada/arquivo_vazio.apid" "$base/saida/arquivo_vazio.txt"
    run_error_case P6 parser "$base/entrada/virgula_ausente.apid" "$base/saida/virgula_ausente.txt"
    run_error_case P7 parser "$base/entrada/virgula_sobrando.apid" "$base/saida/virgula_sobrando.txt"
    run_error_case P8 parser "$base/entrada/lista_incompleta.apid" "$base/saida/lista_incompleta.txt"
    run_error_case P9 parser "$base/entrada/multiplos_erros.apid" "$base/saida/multiplos_erros.txt"
}

run_semantic_tests() {
    print_header "Categoria 2 — Análise semântica"
    local base="$TESTES_DIR/3.casos_teste_semantico_gerador"
    run_error_case S1 semantico "$base/entrada/rota_duplicada.apid" "$base/saida/rota_duplicada.txt"
    run_error_case S2 semantico "$base/entrada/dependencia_circular.apid" "$base/saida/dependencia_circular.txt"
    run_error_case S3 semantico "$base/entrada/tipo_retorno_invalido.apid" "$base/saida/tipo_retorno_invalido.txt"
    run_error_case S4 semantico "$base/entrada/multiplos_erros.apid" "$base/saida/multiplos_erros.txt"
    run_error_case S5 semantico "$base/entrada/tipo_campo_invalido.apid" "$base/saida/tipo_campo_invalido.txt"
    run_error_case S6 semantico "$base/entrada/duplicidades.apid" "$base/saida/duplicidades.txt"
    run_error_case S7 semantico "$base/entrada/falso_ciclo.apid" "$base/saida/falso_ciclo.txt"
    run_error_case S8 semantico "$base/entrada/caminho_invalido.apid" "$base/saida/caminho_invalido.txt"
    run_error_case S9 semantico "$base/entrada/rota_parametro_duplicada.apid" "$base/saida/rota_parametro_duplicada.txt"
    run_error_case S10 semantico "$base/entrada/colisao_metodo.apid" "$base/saida/colisao_metodo.txt"
}

run_generator_tests() {
    print_header "Categoria 3 — Geração de código"
    local base="$TESTES_DIR/3.casos_teste_semantico_gerador"
    run_generation_case G1 "$base/entrada/valido_kotlin.apid" kotlin "$base/saida/valido_kotlin"
    run_generation_case G2 "$base/entrada/valido_typescript.apid" typescript "$base/saida/valido_typescript"
    run_generation_case G3-KT "$base/entrada/lista_generica.apid" kotlin "$base/saida/lista_generica/kotlin"
    run_generation_case G3-TS "$base/entrada/lista_generica.apid" typescript "$base/saida/lista_generica/typescript"
    run_generation_case G4-KT "$base/entrada/cobertura_gerador.apid" kotlin "$base/saida/cobertura_gerador/kotlin"
    run_generation_case G4-TS "$base/entrada/cobertura_gerador.apid" typescript "$base/saida/cobertura_gerador/typescript"
}

run_cli_tests() {
    print_header "Categoria 4 — CLI e arquivos"
    local input="$TESTES_DIR/3.casos_teste_semantico_gerador/entrada/valido_kotlin.apid"
    local log status output

    log="$TMP_ROOT/C1.log"
    java -jar "$JAR_FILE" --target kotlin --output "$TMP_ROOT/missing" "$TMP_ROOT/inexistente.apid" >"$log" 2>&1
    status=$?
    if [ "$status" -ne 0 ] && grep -Fq "Erro ao abrir arquivo: $TMP_ROOT/inexistente.apid" "$log"; then
        pass_test "C1 — arquivo de entrada inexistente"
    else
        fail_test "C1 — arquivo de entrada inexistente"
        show_log "$log"
    fi

    log="$TMP_ROOT/C2.log"
    java -jar "$JAR_FILE" --target ruby --output "$TMP_ROOT/invalid-target" "$input" >"$log" 2>&1
    status=$?
    if [ "$status" -ne 0 ] && grep -Fq -- "--target <kotlin|typescript>" "$log"; then
        pass_test "C2 — target inválido"
    else
        fail_test "C2 — target inválido"
        show_log "$log"
    fi

    output="$TMP_ROOT/output-file"
    touch "$output"
    log="$TMP_ROOT/C3.log"
    java -jar "$JAR_FILE" --target kotlin --output "$output" "$input" >"$log" 2>&1
    status=$?
    if [ "$status" -ne 0 ] && grep -Fq "Erro ao gerar arquivos:" "$log"; then
        pass_test "C3 — output inválido"
    else
        fail_test "C3 — output inválido"
        show_log "$log"
    fi

    output="$TMP_ROOT/no-permission"
    mkdir -p "$output"
    chmod 500 "$output"
    log="$TMP_ROOT/C4.log"
    java -jar "$JAR_FILE" --target kotlin --output "$output" "$input" >"$log" 2>&1
    status=$?
    chmod 700 "$output"
    if [ "$status" -ne 0 ] && grep -Fq "Erro ao gerar arquivos:" "$log"; then
        pass_test "C4 — diretório sem permissão de escrita"
    else
        fail_test "C4 — diretório sem permissão de escrita"
        show_log "$log"
    fi

    output="$TMP_ROOT/stale-output"
    mkdir -p "$output/dtos" "$output/controllers"
    touch "$output/dtos/OldDto.kt" "$output/controllers/OldController.kt"
    log="$TMP_ROOT/C5.log"
    java -jar "$JAR_FILE" --target kotlin --output "$output" "$input" >"$log" 2>&1
    status=$?
    if [ "$status" -eq 0 ] && [ ! -s "$log" ] \
        && [ ! -e "$output/dtos/OldDto.kt" ] \
        && [ ! -e "$output/controllers/OldController.kt" ] \
        && [ -e "$output/dtos/UserDto.kt" ]; then
        pass_test "C5 — remoção de arquivos gerados antigos"
    else
        fail_test "C5 — remoção de arquivos gerados antigos"
        show_log "$log"
    fi
}

main() {
    if ! compile_project; then
        exit 1
    fi

    TMP_ROOT="$(mktemp -d /tmp/apidraft-tests.XXXXXX)"
    trap 'rm -rf "$TMP_ROOT"' EXIT

    case "${1:-todos}" in
        todos)
            run_lexer_tests
            run_parser_tests
            run_semantic_tests
            run_generator_tests
            run_cli_tests
            ;;
        lexer) run_lexer_tests ;;
        parser) run_parser_tests ;;
        semantico)
            run_semantic_tests
            run_generator_tests
            ;;
        gerador) run_generator_tests ;;
        cli) run_cli_tests ;;
        *)
            echo -e "${RED}Etapa inválida: $1${NC}"
            echo "Use: lexer, parser, semantico, gerador ou cli"
            exit 1
            ;;
    esac

    print_header "Resumo final"
    echo -e "Testes aprovados: ${GREEN}$TOTAL_PASSED${NC}"
    echo -e "Testes reprovados: ${RED}$TOTAL_FAILED${NC}"
    if [ "$TOTAL_FAILED" -eq 0 ]; then
        echo -e "${GREEN}100% dos testes executados passaram.${NC}"
        exit 0
    fi
    exit 1
}

main "$@"
