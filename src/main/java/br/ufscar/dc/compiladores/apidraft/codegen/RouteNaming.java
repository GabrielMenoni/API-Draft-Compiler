package br.ufscar.dc.compiladores.apidraft.codegen;

import br.ufscar.dc.compiladores.apidraft.ast.HttpMethod;
import br.ufscar.dc.compiladores.apidraft.ast.RouteNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Regras compartilhadas de validação e nomes derivados de caminhos HTTP. */
public final class RouteNaming {
    private static final Pattern STATIC_SEGMENT =
        Pattern.compile("[A-Za-z][A-Za-z0-9]*(?:[-_][A-Za-z0-9]+)*");
    private static final Pattern BRACED_PARAMETER =
        Pattern.compile("\\{([A-Za-z][A-Za-z0-9_]*)}");
    private static final Pattern COLON_PARAMETER =
        Pattern.compile(":([A-Za-z][A-Za-z0-9_]*)");

    private RouteNaming() {
    }

    public static boolean isValidPath(String path) {
        if ("/".equals(path)) {
            return true;
        }
        if (path == null || !path.startsWith("/") || path.endsWith("/") || path.contains("//")) {
            return false;
        }

        String[] segments = path.substring(1).split("/");
        if (segments.length == 0 || !STATIC_SEGMENT.matcher(segments[0]).matches()) {
            return false;
        }
        for (String segment : segments) {
            if (!STATIC_SEGMENT.matcher(segment).matches() && parameterName(segment) == null) {
                return false;
            }
        }
        return true;
    }

    public static String canonicalPath(String path) {
        return transformParameters(path, ":%s");
    }

    public static String springPath(String path) {
        return transformParameters(path, "{%s}");
    }

    public static String nestPath(String path) {
        return transformParameters(path, ":%s");
    }

    public static String controllerKey(String path) {
        if ("/".equals(path)) {
            return "root";
        }
        return path.substring(1).split("/", 2)[0];
    }

    public static String controllerName(String path) {
        if ("/".equals(path)) {
            return "Root";
        }
        return toPascalCase(controllerKey(path));
    }

    public static String controllerFileName(String path) {
        if ("/".equals(path)) {
            return "root";
        }
        return String.join("-", words(controllerKey(path))).toLowerCase(Locale.ROOT);
    }

    /**
     * Deriva um nome de método a partir do verbo HTTP e do caminho, ex.:
     * {@code GET "/users/{id}"} -> {@code getUserById}, {@code POST "/users"} -> {@code createUser}.
     * Usado tanto para gerar código quanto por {@link br.ufscar.dc.compiladores.apidraft.semantic.SemanticAnalyzer}
     * para detectar rotas distintas que colidiriam no mesmo nome de método gerado.
     */
    public static String methodName(RouteNode route) {
        String action = action(route.method);
        if ("/".equals(route.path)) {
            return action + "Root";
        }

        String[] segments = route.path.substring(1).split("/");
        String first = route.method == HttpMethod.GET
            ? toPascalCase(segments[0])
            : toPascalCase(singularize(segments[0]));
        StringBuilder result = new StringBuilder(action).append(first);

        for (int i = 1; i < segments.length; i++) {
            String parameter = parameterName(segments[i]);
            if (parameter != null) {
                result.append("By").append(toPascalCase(parameter));
            } else {
                result.append(toPascalCase(segments[i]));
            }
        }
        return result.toString();
    }

    private static String action(HttpMethod method) {
        switch (method) {
            case GET:    return "get";
            case POST:   return "create";
            case PUT:    return "update";
            case DELETE: return "delete";
            case PATCH:  return "patch";
            default:     return "handle";
        }
    }

    private static String singularize(String segment) {
        if (segment.endsWith("s") && segment.length() > 1) {
            return segment.substring(0, segment.length() - 1);
        }
        return segment;
    }

    private static String toPascalCase(String value) {
        StringBuilder result = new StringBuilder();
        for (String word : words(value)) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
        }
        return result.toString();
    }

    private static List<String> words(String value) {
        List<String> result = new ArrayList<>();
        for (String word : value.split("[-_]")) {
            if (!word.isEmpty()) {
                result.add(word);
            }
        }
        return result;
    }

    private static String parameterName(String segment) {
        Matcher braced = BRACED_PARAMETER.matcher(segment);
        if (braced.matches()) {
            return braced.group(1);
        }
        Matcher colon = COLON_PARAMETER.matcher(segment);
        return colon.matches() ? colon.group(1) : null;
    }

    private static String transformParameters(String path, String format) {
        if (path == null || "/".equals(path)) {
            return path;
        }
        String[] segments = path.split("/", -1);
        for (int i = 0; i < segments.length; i++) {
            String parameter = parameterName(segments[i]);
            if (parameter != null) {
                segments[i] = String.format(format, parameter);
            }
        }
        return String.join("/", segments);
    }
}
