package br.ufscar.dc.compiladores.apidraft.semantic;

/** Um erro semântico encontrado por {@link SemanticAnalyzer}, com a linha onde ocorreu. */
public class SemanticError {
    public final String message;
    public final int line;

    public SemanticError(String message, int line) {
        this.message = message;
        this.line = line;
    }
}
