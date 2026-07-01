package br.ufscar.dc.compiladores.apidraft.semantic;

import br.ufscar.dc.compiladores.apidraft.ast.*;

import java.util.*;

public class SemanticAnalyzer {

    private static final Set<String> PRIMITIVES = Set.of("string", "int", "bool", "float");

    public List<String> analyze(ProgramNode program) {
        List<SemanticError> errors = new ArrayList<>();
        errors.addAll(checkReturnTypes(program));
        errors.addAll(checkDuplicateRoutes(program));
        errors.addAll(checkCircularDependencies(program));

        List<String> messages = new ArrayList<>();
        for (SemanticError e : errors) {
            messages.add(e.message);
        }
        return messages;
    }

    // V3: return type must be a declared entity or a primitive
    private List<SemanticError> checkReturnTypes(ProgramNode program) {
        Set<String> declaredEntities = new HashSet<>();
        for (EntityNode entity : program.entities) {
            declaredEntities.add(entity.name);
        }

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

    // V1: duplicate route = same method + path
    private List<SemanticError> checkDuplicateRoutes(ProgramNode program) {
        Map<String, Integer> seen = new LinkedHashMap<>();
        List<SemanticError> errors = new ArrayList<>();

        for (RouteNode route : program.routes) {
            String key = route.method + " \"" + route.path + "\"";
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

    // V2: circular dependency between entities (DFS white/gray/black)
    private List<SemanticError> checkCircularDependencies(ProgramNode program) {
        Set<String> entityNames = new HashSet<>();
        for (EntityNode entity : program.entities) {
            entityNames.add(entity.name);
        }

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
            graph.put(entity.name, deps);
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
                return; // report one cycle per component
            }
            if (color.getOrDefault(neighbor, 0) == 0) {
                dfs(neighbor, graph, color, path, errors);
                if (!errors.isEmpty()) return; // stop after first cycle found in this branch
            }
        }

        path.removeLast();
        color.put(node, 2); // black
    }

    private String resolveBaseType(TypeNode type) {
        if ("List".equals(type.baseType) && type.genericArg != null) {
            return resolveBaseType(type.genericArg);
        }
        return type.baseType;
    }
}
