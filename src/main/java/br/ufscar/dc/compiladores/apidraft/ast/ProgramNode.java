package br.ufscar.dc.compiladores.apidraft.ast;

import java.util.List;

/**
 * Raiz da AST de um arquivo {@code .apid}: todas as entidades e rotas declaradas,
 * na ordem em que aparecem no arquivo-fonte.
 */
public class ProgramNode {
    public final List<EntityNode> entities;
    public final List<RouteNode> routes;

    public ProgramNode(List<EntityNode> entities, List<RouteNode> routes) {
        this.entities = entities;
        this.routes = routes;
    }
}
