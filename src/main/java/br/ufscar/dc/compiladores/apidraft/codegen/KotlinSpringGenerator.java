package br.ufscar.dc.compiladores.apidraft.codegen;

import br.ufscar.dc.compiladores.apidraft.ast.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class KotlinSpringGenerator implements CodeGenerator {

    @Override
    public void generate(ProgramNode program, Path outputDir) throws IOException {
        Path dtosDir = outputDir.resolve("dtos");
        Path controllersDir = outputDir.resolve("controllers");
        GeneratedOutput.recreateDirectory(dtosDir);
        GeneratedOutput.recreateDirectory(controllersDir);

        for (EntityNode entity : program.entities) {
            generateDto(entity, dtosDir);
        }

        Map<String, List<RouteNode>> groups = groupByPathPrefix(program.routes);
        for (Map.Entry<String, List<RouteNode>> entry : groups.entrySet()) {
            generateController(entry.getKey(), entry.getValue(), program.entities, controllersDir);
        }
    }

    private void generateDto(EntityNode entity, Path dtosDir) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("package com.example.generated.dtos\n\n");
        sb.append("data class ").append(entity.name).append("Dto(\n");

        List<String> params = new ArrayList<>();
        for (FieldNode field : entity.fields) {
            params.add("    val " + field.fieldName + ": " + toKotlinType(field.fieldType));
        }
        sb.append(String.join(",\n", params));
        sb.append("\n)\n");

        Path file = dtosDir.resolve(entity.name + "Dto.kt");
        Files.writeString(file, sb.toString());
    }

    private void generateController(String prefix, List<RouteNode> routes,
                                    List<EntityNode> entities, Path controllersDir) throws IOException {
        String className = RouteNaming.controllerName(routes.get(0).path) + "Controller";

        Set<String> imports = new LinkedHashSet<>();
        for (RouteNode route : routes) {
            String baseType = resolveBaseType(route.returnType);
            if (!isPrimitive(baseType)) {
                imports.add("import com.example.generated.dtos." + baseType + "Dto");
            }
        }
        imports.add("import org.springframework.web.bind.annotation.*");

        StringBuilder sb = new StringBuilder();
        sb.append("package com.example.generated.controllers\n\n");
        for (String imp : imports) {
            sb.append(imp).append("\n");
        }
        sb.append("\n@RestController\n@RequestMapping\n");
        sb.append("class ").append(className).append(" {\n");

        for (RouteNode route : routes) {
            sb.append("\n");
            sb.append("    @").append(toSpringAnnotation(route.method)).append("(\"")
                .append(RouteNaming.springPath(route.path)).append("\")\n");
            String methodName = RouteNaming.methodName(route);
            String returnType = toKotlinReturnType(route.returnType);
            sb.append("    fun ").append(methodName).append("(): ").append(returnType).append(" {\n");
            sb.append("        TODO(\"Not yet implemented\")\n");
            sb.append("    }\n");
        }

        sb.append("}\n");

        Path file = controllersDir.resolve(className + ".kt");
        Files.writeString(file, sb.toString());
    }

    private Map<String, List<RouteNode>> groupByPathPrefix(List<RouteNode> routes) {
        Map<String, List<RouteNode>> groups = new LinkedHashMap<>();
        for (RouteNode route : routes) {
            String prefix = RouteNaming.controllerKey(route.path);
            groups.computeIfAbsent(prefix, k -> new ArrayList<>()).add(route);
        }
        return groups;
    }

    private String toKotlinType(TypeNode type) {
        if ("List".equals(type.baseType) && type.genericArg != null) {
            return "List<" + toKotlinType(type.genericArg) + ">";
        }
        return mapKotlinPrimitive(type.baseType);
    }

    private String toKotlinReturnType(TypeNode type) {
        if ("List".equals(type.baseType) && type.genericArg != null) {
            return "List<" + toKotlinReturnType(type.genericArg) + ">";
        }
        if (isPrimitive(type.baseType)) {
            return mapKotlinPrimitive(type.baseType);
        }
        return type.baseType + "Dto";
    }

    private String mapKotlinPrimitive(String type) {
        switch (type) {
            case "string": return "String";
            case "int":    return "Int";
            case "bool":   return "Boolean";
            case "float":  return "Float";
            default:       return type + "Dto";
        }
    }

    private String toSpringAnnotation(HttpMethod method) {
        switch (method) {
            case GET:    return "GetMapping";
            case POST:   return "PostMapping";
            case PUT:    return "PutMapping";
            case DELETE: return "DeleteMapping";
            case PATCH:  return "PatchMapping";
            default:     return "RequestMapping";
        }
    }

    private String resolveBaseType(TypeNode type) {
        if ("List".equals(type.baseType) && type.genericArg != null) {
            return resolveBaseType(type.genericArg);
        }
        return type.baseType;
    }

    private boolean isPrimitive(String type) {
        return type.equals("string") || type.equals("int") || type.equals("bool") || type.equals("float");
    }

}
