package br.ufscar.dc.compiladores.apidraft.ast;

import java.util.List;

public class EntityNode {
    public final String name;
    public final List<FieldNode> fields;
    public final int line;

    public EntityNode(String name, List<FieldNode> fields, int line) {
        this.name = name;
        this.fields = fields;
        this.line = line;
    }

    public EntityNode(String name, List<FieldNode> fields) {
        this(name, fields, 0);
    }
}
