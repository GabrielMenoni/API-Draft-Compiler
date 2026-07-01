package br.ufscar.dc.compiladores.apidraft.ast;

public class RouteNode {
    public final HttpMethod method;
    public final String path;
    public final TypeNode returnType;

    public RouteNode(HttpMethod method, String path, TypeNode returnType) {
        this.method = method;
        this.path = path;
        this.returnType = returnType;
    }
}
