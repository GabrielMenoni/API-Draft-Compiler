package br.ufscar.dc.compiladores.apidraft.ast;

public class FieldNode {
    public final TypeNode fieldType;
    public final String fieldName;

    public FieldNode(TypeNode fieldType, String fieldName) {
        this.fieldType = fieldType;
        this.fieldName = fieldName;
    }
}
