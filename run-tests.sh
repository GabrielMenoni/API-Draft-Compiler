#!/bin/bash

# Script para executar testes do compilador API-Draft
# Uso: ./run-tests.sh [lexer|parser|semantico]
# Exemplos:
#   ./run-tests.sh          # Executa todas as etapas
#   ./run-tests.sh lexer    # Executa apenas testes léxicos
#   ./run-tests.sh parser   # Executa apenas testes sintáticos
#   ./run-tests.sh semantico # Executa apenas testes semânticos/geração

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TESTES_DIR="$SCRIPT_DIR/testes"
JAR_FILE="$SCRIPT_DIR/target/api-draft-compiler-jar-with-dependencies.jar"

# Auto-detectar JAVA_HOME se não estiver definido ou estiver inválido
if [ -z "$JAVA_HOME" ] || [ ! -x "$JAVA_HOME/bin/java" ]; then
    if command -v java &> /dev/null; then
        JAVA_BIN=$(readlink -f "$(command -v java)")
        JAVA_HOME=$(dirname "$(dirname "$JAVA_BIN")")
        export JAVA_HOME
        export PATH="$JAVA_HOME/bin:$PATH"
    fi
fi

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

print_header() {
    echo -e "${YELLOW}========================================${NC}"
    echo -e "${YELLOW}$1${NC}"
    echo -e "${YELLOW}========================================${NC}"
}

compile_project() {
    print_header "Compilando projeto..."
    cd "$SCRIPT_DIR"
    mvn clean package -q -DskipTests || {
        echo -e "${RED}Erro ao compilar o projeto${NC}"
        exit 1
    }
    echo -e "${GREEN}✓ Projeto compilado com sucesso${NC}"
    echo ""
}

run_test() {
    local etapa=$1
    local entrada=$2
    local esperada=$3
    local saida_temp="/tmp/apidraft_test_output_$$.txt"
    local test_name
    test_name=$(basename "$entrada")

    java -jar "$JAR_FILE" --target kotlin --output /tmp "$entrada" > "$saida_temp" 2>&1 || true

    if diff -q "$esperada" "$saida_temp" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ PASS${NC}: $etapa/$test_name"
        rm -f "$saida_temp"
        return 0
    else
        echo -e "${RED}✗ FAIL${NC}: $etapa/$test_name"
        echo "  Esperado:"
        head -3 "$esperada" | sed 's/^/    /'
        echo "  ---"
        echo "  Obtido:"
        head -3 "$saida_temp" | sed 's/^/    /'
        rm -f "$saida_temp"
        return 1
    fi
}

run_etapa() {
    local etapa=$1
    local etapa_dir="$TESTES_DIR/testComp/$etapa"
    local entrada_dir="$etapa_dir/entrada"
    local saida_dir="$etapa_dir/saida"

    if [ ! -d "$entrada_dir" ] || [ ! -d "$saida_dir" ]; then
        echo -e "${YELLOW}Nenhum teste encontrado para $etapa${NC}"
        return 0
    fi

    print_header "Executando testes: $etapa"

    local passed=0
    local failed=0
    local test_count=0

    for entrada in "$entrada_dir"/*; do
        [ -f "$entrada" ] || continue
        test_count=$((test_count + 1))
        local case_name
        case_name=$(basename "$entrada")
        local esperada="$saida_dir/$case_name"

        if [ ! -f "$esperada" ]; then
            echo -e "${RED}✗ FAIL${NC}: $etapa/$case_name (arquivo de saída esperada não encontrado)"
            failed=$((failed + 1))
        else
            if run_test "$etapa" "$entrada" "$esperada"; then
                passed=$((passed + 1))
            else
                failed=$((failed + 1))
            fi
        fi
    done

    if [ $test_count -eq 0 ]; then
        echo -e "${YELLOW}Nenhum teste encontrado em $etapa${NC}"
    else
        echo ""
        echo -e "Resultados: ${GREEN}$passed passou${NC}, ${RED}$failed falharam${NC} (total: $test_count)"
        echo ""
    fi

    return $([ $failed -eq 0 ] && echo 0 || echo 1)
}

map_arg_para_pasta() {
    case "$1" in
        lexer)     echo "1.casos_teste_lexer" ;;
        parser)    echo "2.casos_teste_parser" ;;
        semantico) echo "3.casos_teste_semantico_gerador" ;;
        *)         echo "" ;;
    esac
}

main() {
    compile_project

    local total_passed=0
    local total_failed=0

    if [ -z "$1" ]; then
        print_header "Executando todos os testes"
        for etapa in "1.casos_teste_lexer" "2.casos_teste_parser" "3.casos_teste_semantico_gerador"; do
            if run_etapa "$etapa"; then
                total_passed=$((total_passed + 1))
            else
                total_failed=$((total_failed + 1))
            fi
        done
        print_header "Resumo Final"
        echo -e "Etapas com sucesso: ${GREEN}$total_passed${NC}"
        echo -e "Etapas com falha:   ${RED}$total_failed${NC}"
    else
        local pasta
        pasta="$(map_arg_para_pasta "$1")"
        if [ -z "$pasta" ]; then
            echo -e "${RED}Etapa inválida: $1${NC}"
            echo "Use: lexer, parser, ou semantico"
            exit 1
        fi
        run_etapa "$pasta"
    fi
}

if [ ! -f "$JAR_FILE" ]; then
    compile_project
fi

main "$@"
