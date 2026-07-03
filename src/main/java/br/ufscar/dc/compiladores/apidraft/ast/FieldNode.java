package br.ufscar.dc.compiladores.apidraft.ast;

/** Nó de AST para um campo de entidade (ex.: {@code string name}). */
public class FieldNode {
    public final TypeNode fieldType;
    public final String fieldName;
    public final int line;

    public FieldNode(TypeNode fieldType, String fieldName, int line) {
        this.fieldType = fieldType;
        this.fieldName = fieldName;
        this.line = line;
    }

    public FieldNode(TypeNode fieldType, String fieldName) {
        this(fieldType, fieldName, 0);
    }
}
