package br.ufscar.dc.compiladores.apidraft.ast;

public class TypeNode {
    public final String baseType;
    public final TypeNode genericArg;

    public TypeNode(String baseType, TypeNode genericArg) {
        this.baseType = baseType;
        this.genericArg = genericArg;
    }

    public TypeNode(String baseType) {
        this(baseType, null);
    }

    @Override
    public String toString() {
        if (genericArg != null) {
            return "List<" + genericArg + ">";
        }
        return baseType;
    }
}
