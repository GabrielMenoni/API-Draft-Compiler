package br.ufscar.dc.compiladores.apidraft;

import br.ufscar.dc.compiladores.apidraft.ast.*;

import java.util.ArrayList;
import java.util.List;

/** Converte a árvore de parse gerada pelo ANTLR na AST tipada usada pelas fases seguintes. */
public class AstBuilder extends ApiDraftParserBaseVisitor<Object> {

    @Override
    public ProgramNode visitPrograma(ApiDraftParser.ProgramaContext ctx) {
        List<EntityNode> entities = new ArrayList<>();
        List<RouteNode> routes = new ArrayList<>();

        for (ApiDraftParser.DeclaracaoContext decl : ctx.declaracao()) {
            if (decl.entityDecl() != null) {
                entities.add(visitEntityDecl(decl.entityDecl()));
            } else if (decl.routeDecl() != null) {
                routes.add(visitRouteDecl(decl.routeDecl()));
            }
        }

        return new ProgramNode(entities, routes);
    }

    @Override
    public EntityNode visitEntityDecl(ApiDraftParser.EntityDeclContext ctx) {
        String name = ctx.IDENT().getText();
        List<FieldNode> fields = visitFieldList(ctx.fieldList());
        return new EntityNode(name, fields, ctx.getStart().getLine());
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<FieldNode> visitFieldList(ApiDraftParser.FieldListContext ctx) {
        List<FieldNode> fields = new ArrayList<>();
        for (ApiDraftParser.FieldContext f : ctx.field()) {
            fields.add(visitField(f));
        }
        return fields;
    }

    @Override
    public FieldNode visitField(ApiDraftParser.FieldContext ctx) {
        TypeNode type = visitTipo(ctx.tipo());
        String name = ctx.IDENT().getText();
        return new FieldNode(type, name, ctx.getStart().getLine());
    }

    @Override
    public RouteNode visitRouteDecl(ApiDraftParser.RouteDeclContext ctx) {
        HttpMethod method = visitMetodoHttp(ctx.metodoHttp());
        // Strip surrounding quotes from PATH token
        String rawPath = ctx.PATH().getText();
        String path = rawPath.substring(1, rawPath.length() - 1);
        TypeNode returnType = visitTipo(ctx.tipo());
        int line = ctx.getStart().getLine();
        return new RouteNode(method, path, returnType, line);
    }

    @Override
    public HttpMethod visitMetodoHttp(ApiDraftParser.MetodoHttpContext ctx) {
        String text = ctx.getStart().getText();
        return HttpMethod.valueOf(text);
    }

    @Override
    public TypeNode visitTipo(ApiDraftParser.TipoContext ctx) {
        if (ctx.LIST() != null) {
            TypeNode inner = visitTipo(ctx.tipo());
            return new TypeNode("List", inner);
        }
        return new TypeNode(ctx.getStart().getText());
    }
}
