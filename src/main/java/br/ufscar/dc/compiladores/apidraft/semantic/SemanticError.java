package br.ufscar.dc.compiladores.apidraft.semantic;

public class SemanticError {
    public final String message;
    public final int line;

    public SemanticError(String message, int line) {
        this.message = message;
        this.line = line;
    }
}
