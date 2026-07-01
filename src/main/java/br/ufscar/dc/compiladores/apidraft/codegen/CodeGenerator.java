package br.ufscar.dc.compiladores.apidraft.codegen;

import br.ufscar.dc.compiladores.apidraft.ast.ProgramNode;

import java.io.IOException;
import java.nio.file.Path;

public interface CodeGenerator {
    void generate(ProgramNode program, Path outputDir) throws IOException;
}
