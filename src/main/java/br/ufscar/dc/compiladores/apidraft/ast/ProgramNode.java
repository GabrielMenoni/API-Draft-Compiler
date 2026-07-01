package br.ufscar.dc.compiladores.apidraft.ast;

import java.util.List;

public class ProgramNode {
    public final List<EntityNode> entities;
    public final List<RouteNode> routes;

    public ProgramNode(List<EntityNode> entities, List<RouteNode> routes) {
        this.entities = entities;
        this.routes = routes;
    }
}
