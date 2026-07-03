package br.ufscar.dc.compiladores.apidraft.ast;

import java.util.List;

/**
 * Nó de AST para uma declaração {@code ENTITY} (nome + lista de campos).
 * Cada entidade vira um DTO no código gerado (ex.: {@code UserDto}).
 */
public class EntityNode {
    public final String name;
    public final List<FieldNode> fields;
    public final int line;

    public EntityNode(String name, List<FieldNode> fields, int line) {
        this.name = name;
        this.fields = fields;
        this.line = line;
    }

    /** Usado apenas em testes que constroem a AST manualmente, sem posição no arquivo-fonte. */
    public EntityNode(String name, List<FieldNode> fields) {
        this(name, fields, 0);
    }
}
