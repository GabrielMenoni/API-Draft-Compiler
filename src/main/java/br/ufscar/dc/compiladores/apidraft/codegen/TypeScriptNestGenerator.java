package br.ufscar.dc.compiladores.apidraft.codegen;

import br.ufscar.dc.compiladores.apidraft.ast.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class TypeScriptNestGenerator implements CodeGenerator {

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
            generateController(entry.getKey(), entry.getValue(), controllersDir);
        }
    }

    private void generateDto(EntityNode entity, Path dtosDir) throws IOException {
        StringBuilder sb = new StringBuilder();
        Set<String> imports = new LinkedHashSet<>();
        for (FieldNode field : entity.fields) {
            String baseType = resolveBaseType(field.fieldType);
            if (!isPrimitive(baseType) && !baseType.equals(entity.name)) {
                imports.add("import { " + baseType + "Dto } from './" + toKebabCase(baseType) + ".dto';");
            }
        }
        for (String dtoImport : imports) {
            sb.append(dtoImport).append("\n");
        }
        if (!imports.isEmpty()) {
            sb.append("\n");
        }
        sb.append("export class ").append(entity.name).append("Dto {\n");

        for (FieldNode field : entity.fields) {
            sb.append("  ").append(field.fieldName).append("!: ").append(toTsType(field.fieldType)).append(";\n");
        }
        sb.append("}\n");

        String fileName = toKebabCase(entity.name) + ".dto.ts";
        Path file = dtosDir.resolve(fileName);
        Files.writeString(file, sb.toString());
    }

    private void generateController(String prefix, List<RouteNode> routes, Path controllersDir) throws IOException {
        String className = RouteNaming.controllerName(routes.get(0).path) + "Controller";

        // Collect NestJS decorator imports
        Set<String> decorators = new LinkedHashSet<>();
        decorators.add("Controller");
        for (RouteNode route : routes) {
            decorators.add(toNestDecorator(route.method));
        }

        // Collect DTO imports
        Set<String> dtoImports = new LinkedHashSet<>();
        for (RouteNode route : routes) {
            String baseType = resolveBaseType(route.returnType);
            if (!isPrimitive(baseType)) {
                String dtoFile = "../dtos/" + toKebabCase(baseType) + ".dto";
                dtoImports.add("import { " + baseType + "Dto } from '" + dtoFile + "';");
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("import { ").append(String.join(", ", decorators)).append(" } from '@nestjs/common';\n");
        for (String imp : dtoImports) {
            sb.append(imp).append("\n");
        }
        sb.append("\n@Controller()\n");
        sb.append("export class ").append(className).append(" {\n");

        for (RouteNode route : routes) {
            sb.append("\n");
            sb.append("  @").append(toNestDecorator(route.method)).append("('")
                .append(RouteNaming.nestPath(route.path)).append("')\n");
            String methodName = RouteNaming.methodName(route);
            String returnType = toTsReturnType(route.returnType);
            sb.append("  ").append(methodName).append("(): ").append(returnType).append(" {\n");
            sb.append("    throw new Error('Not implemented');\n");
            sb.append("  }\n");
        }

        sb.append("}\n");

        String fileName = RouteNaming.controllerFileName(routes.get(0).path) + ".controller.ts";
        Path file = controllersDir.resolve(fileName);
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

    private String toTsType(TypeNode type) {
        if ("List".equals(type.baseType) && type.genericArg != null) {
            return toTsType(type.genericArg) + "[]";
        }
        if (isPrimitive(type.baseType)) {
            return mapTsPrimitive(type.baseType);
        }
        return type.baseType + "Dto";
    }

    private String toTsReturnType(TypeNode type) {
        if ("List".equals(type.baseType) && type.genericArg != null) {
            return toTsReturnType(type.genericArg) + "[]";
        }
        if (isPrimitive(type.baseType)) {
            return mapTsPrimitive(type.baseType);
        }
        return type.baseType + "Dto";
    }

    private String mapTsPrimitive(String type) {
        switch (type) {
            case "string": return "string";
            case "int":    return "number";
            case "bool":   return "boolean";
            case "float":  return "number";
            default:       return type;
        }
    }

    private String toNestDecorator(HttpMethod method) {
        switch (method) {
            case GET:    return "Get";
            case POST:   return "Post";
            case PUT:    return "Put";
            case DELETE: return "Delete";
            case PATCH:  return "Patch";
            default:     return "Get";
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

    // "UserProfile" → "user-profile"
    private String toKebabCase(String name) {
        return name.replaceAll("([A-Z])", "-$1").toLowerCase().replaceAll("^-", "");
    }

}
