package br.ufscar.dc.compiladores.apidraft.ast;

/** Nó de AST para uma declaração {@code ROUTE} (método HTTP + caminho + tipo de retorno). */
public class RouteNode {
    public final HttpMethod method;
    public final String path;
    public final TypeNode returnType;
    public final int line;

    public RouteNode(HttpMethod method, String path, TypeNode returnType, int line) {
        this.method = method;
        this.path = path;
        this.returnType = returnType;
        this.line = line;
    }
}
