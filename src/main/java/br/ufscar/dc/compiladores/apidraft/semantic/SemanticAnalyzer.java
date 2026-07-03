package br.ufscar.dc.compiladores.apidraft.semantic;

import br.ufscar.dc.compiladores.apidraft.ast.*;
import br.ufscar.dc.compiladores.apidraft.codegen.RouteNaming;

import java.util.*;

/**
 * Verificações de conformidade que a gramática não consegue expressar. Além das
 * checagens estruturais básicas (entidade/campo duplicado, tipo não declarado),
 * inclui duas análises específicas do domínio de geração de código: colisão de
 * símbolos gerados (duas rotas distintas que produziriam o mesmo nome de
 * controller/método) e dependência circular entre entidades (DFS com coloração
 * branco/cinza/preto). Todos os erros de um programa são acumulados e retornados
 * juntos, em vez de parar no primeiro.
 */
public class SemanticAnalyzer {

    private static final Set<String> PRIMITIVES = Set.of("string", "int", "bool", "float");

    public List<String> analyze(ProgramNode program) {
        List<SemanticError> errors = new ArrayList<>();
        errors.addAll(checkDuplicateEntities(program));
        errors.addAll(checkDuplicateFields(program));
        errors.addAll(checkFieldTypes(program));
        errors.addAll(checkReturnTypes(program));
        errors.addAll(checkPaths(program));
        errors.addAll(checkDuplicateRoutes(program));
        errors.addAll(checkGeneratedSymbolCollisions(program));
        errors.addAll(checkCircularDependencies(program));

        List<String> messages = new ArrayList<>();
        for (SemanticError e : errors) {
            messages.add(e.message);
        }
        return messages;
    }

    private List<SemanticError> checkDuplicateEntities(ProgramNode program) {
        Map<String, Integer> seen = new LinkedHashMap<>();
        List<SemanticError> errors = new ArrayList<>();
        for (EntityNode entity : program.entities) {
            Integer firstLine = seen.putIfAbsent(entity.name, entity.line);
            if (firstLine != null) {
                errors.add(new SemanticError(String.format(
                    "Erro semântico: entidade '%s' duplicada (já declarada na linha %d)",
                    entity.name, firstLine), entity.line));
            }
        }
        return errors;
    }

    private List<SemanticError> checkDuplicateFields(ProgramNode program) {
        List<SemanticError> errors = new ArrayList<>();
        for (EntityNode entity : program.entities) {
            Map<String, Integer> seen = new LinkedHashMap<>();
            for (FieldNode field : entity.fields) {
                Integer firstLine = seen.putIfAbsent(field.fieldName, field.line);
                if (firstLine != null) {
                    errors.add(new SemanticError(String.format(
                        "Erro semântico: campo '%s' duplicado na entidade '%s' (já declarado na linha %d)",
                        field.fieldName, entity.name, firstLine), field.line));
                }
            }
        }
        return errors;
    }

    private List<SemanticError> checkFieldTypes(ProgramNode program) {
        Set<String> declaredEntities = declaredEntities(program);
        List<SemanticError> errors = new ArrayList<>();
        for (EntityNode entity : program.entities) {
            for (FieldNode field : entity.fields) {
                String baseType = resolveBaseType(field.fieldType);
                if (!PRIMITIVES.contains(baseType) && !declaredEntities.contains(baseType)) {
                    errors.add(new SemanticError(String.format(
                        "Erro semântico: tipo '%s' do campo '%s' na entidade '%s' não declarado",
                        baseType, field.fieldName, entity.name), field.line));
                }
            }
        }
        return errors;
    }

    // V3: return type must be a declared entity or a primitive
    private List<SemanticError> checkReturnTypes(ProgramNode program) {
        Set<String> declaredEntities = declaredEntities(program);

        List<SemanticError> errors = new ArrayList<>();
        for (RouteNode route : program.routes) {
            String baseType = resolveBaseType(route.returnType);
            if (!PRIMITIVES.contains(baseType) && !declaredEntities.contains(baseType)) {
                String msg = String.format(
                    "Erro semântico: tipo de retorno '%s' não declarado antes da rota %s \"%s\"",
                    baseType, route.method, route.path
                );
                errors.add(new SemanticError(msg, route.line));
            }
        }
        return errors;
    }

    private List<SemanticError> checkPaths(ProgramNode program) {
        List<SemanticError> errors = new ArrayList<>();
        for (RouteNode route : program.routes) {
            if (!RouteNaming.isValidPath(route.path)) {
                errors.add(new SemanticError(String.format(
                    "Erro semântico: caminho HTTP inválido '%s' na rota %s",
                    route.path, route.method), route.line));
            }
        }
        return errors;
    }

    // V1: duplicate route = same method + path
    private List<SemanticError> checkDuplicateRoutes(ProgramNode program) {
        Map<String, Integer> seen = new LinkedHashMap<>();
        List<SemanticError> errors = new ArrayList<>();

        for (RouteNode route : program.routes) {
            String key = route.method + " \"" + RouteNaming.canonicalPath(route.path) + "\"";
            if (seen.containsKey(key)) {
                String msg = String.format(
                    "Erro semântico: rota duplicada %s (já declarada na linha %d)",
                    key, seen.get(key)
                );
                errors.add(new SemanticError(msg, route.line));
            } else {
                seen.put(key, route.line);
            }
        }
        return errors;
    }

    private List<SemanticError> checkGeneratedSymbolCollisions(ProgramNode program) {
        Map<String, RouteNode> controllers = new LinkedHashMap<>();
        Map<String, RouteNode> methods = new LinkedHashMap<>();
        List<SemanticError> errors = new ArrayList<>();

        for (RouteNode route : program.routes) {
            if (!RouteNaming.isValidPath(route.path)) {
                continue;
            }

            String controllerName = RouteNaming.controllerName(route.path) + "Controller";
            RouteNode firstController = controllers.putIfAbsent(controllerName, route);
            if (firstController != null
                && !RouteNaming.controllerKey(firstController.path).equals(RouteNaming.controllerKey(route.path))) {
                errors.add(new SemanticError(String.format(
                    "Erro semântico: os caminhos '%s' e '%s' geram o mesmo controller '%s'",
                    firstController.path, route.path, controllerName), route.line));
            }

            String methodName = RouteNaming.methodName(route);
            String methodKey = controllerName + "#" + methodName;
            RouteNode firstMethod = methods.putIfAbsent(methodKey, route);
            if (firstMethod != null) {
                boolean sameRoute = firstMethod.method == route.method
                    && RouteNaming.canonicalPath(firstMethod.path).equals(RouteNaming.canonicalPath(route.path));
                if (!sameRoute) {
                    errors.add(new SemanticError(String.format(
                        "Erro semântico: as rotas %s \"%s\" e %s \"%s\" geram o mesmo método '%s'",
                        firstMethod.method, firstMethod.path, route.method, route.path, methodName), route.line));
                }
            }
        }
        return errors;
    }

    // V2: circular dependency between entities (DFS white/gray/black)
    private List<SemanticError> checkCircularDependencies(ProgramNode program) {
        Set<String> entityNames = declaredEntities(program);

        // Build dependency graph: only edges to other entities (not primitives)
        Map<String, Set<String>> graph = new LinkedHashMap<>();
        for (EntityNode entity : program.entities) {
            Set<String> deps = new LinkedHashSet<>();
            for (FieldNode field : entity.fields) {
                String baseType = resolveBaseType(field.fieldType);
                if (entityNames.contains(baseType)) {
                    deps.add(baseType);
                }
            }
            graph.computeIfAbsent(entity.name, key -> new LinkedHashSet<>()).addAll(deps);
        }

        // DFS coloring
        Map<String, Integer> color = new HashMap<>(); // 0=white, 1=gray, 2=black
        for (String name : graph.keySet()) {
            color.put(name, 0);
        }

        List<SemanticError> errors = new ArrayList<>();
        Deque<String> path = new ArrayDeque<>();

        for (String start : graph.keySet()) {
            if (color.get(start) == 0) {
                dfs(start, graph, color, path, errors);
            }
        }
        return errors;
    }

    private void dfs(String node, Map<String, Set<String>> graph,
                     Map<String, Integer> color, Deque<String> path,
                     List<SemanticError> errors) {
        color.put(node, 1); // gray
        path.addLast(node);

        for (String neighbor : graph.getOrDefault(node, Collections.emptySet())) {
            if (color.getOrDefault(neighbor, 0) == 1) {
                // Found a back-edge: reconstruct the cycle from the path
                List<String> cycle = new ArrayList<>();
                boolean inCycle = false;
                for (String n : path) {
                    if (n.equals(neighbor)) inCycle = true;
                    if (inCycle) cycle.add(n);
                }
                cycle.add(neighbor); // close the cycle
                String msg = "Erro semântico: dependência circular detectada entre " + String.join(" -> ", cycle);
                errors.add(new SemanticError(msg, 0));
            } else if (color.getOrDefault(neighbor, 0) == 0) {
                dfs(neighbor, graph, color, path, errors);
            }
        }

        path.removeLast();
        color.put(node, 2); // black
    }

    private Set<String> declaredEntities(ProgramNode program) {
        Set<String> declared = new LinkedHashSet<>();
        for (EntityNode entity : program.entities) {
            declared.add(entity.name);
        }
        return declared;
    }

    private String resolveBaseType(TypeNode type) {
        if ("List".equals(type.baseType) && type.genericArg != null) {
            return resolveBaseType(type.genericArg);
        }
        return type.baseType;
    }
}
