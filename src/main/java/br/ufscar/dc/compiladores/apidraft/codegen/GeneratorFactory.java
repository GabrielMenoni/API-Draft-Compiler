package br.ufscar.dc.compiladores.apidraft.codegen;

public class GeneratorFactory {

    public static CodeGenerator create(String target) {
        switch (target) {
            case "kotlin":     return new KotlinSpringGenerator();
            case "typescript": return new TypeScriptNestGenerator();
            default:
                throw new IllegalArgumentException("Target inválido: '" + target + "'. Use 'kotlin' ou 'typescript'.");
        }
    }
}
