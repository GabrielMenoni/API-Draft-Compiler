package br.ufscar.dc.compiladores.apidraft.codegen;

import br.ufscar.dc.compiladores.apidraft.ast.ProgramNode;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Contrato implementado por cada back-end suportado (Kotlin/Spring, TypeScript/NestJS).
 * Ver {@link GeneratorFactory} para a escolha da implementação a partir do parâmetro
 * {@code --target} da CLI.
 */
public interface CodeGenerator {
    /** Escreve DTOs e controllers derivados de {@code program} em {@code outputDir}. */
    void generate(ProgramNode program, Path outputDir) throws IOException;
}
