package br.ufscar.dc.compiladores.apidraft.ast;

import java.util.List;

public class EntityNode {
    public final String name;
    public final List<FieldNode> fields;

    public EntityNode(String name, List<FieldNode> fields) {
        this.name = name;
        this.fields = fields;
    }
}
