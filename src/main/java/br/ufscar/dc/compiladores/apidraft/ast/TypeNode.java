package br.ufscar.dc.compiladores.apidraft.ast;

/**
 * Tipo de um campo ou retorno de rota: um primitivo, o nome de uma entidade,
 * ou {@code List<T>}. {@code List<T>} é o único tipo genérico da linguagem,
 * então basta um único campo {@link #genericArg} (não nulo apenas quando
 * {@link #baseType} é {@code "List"}) em vez de uma lista de argumentos.
 */
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
