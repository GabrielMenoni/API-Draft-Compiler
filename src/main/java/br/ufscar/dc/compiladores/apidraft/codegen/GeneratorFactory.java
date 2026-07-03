package br.ufscar.dc.compiladores.apidraft.codegen;

/** Resolve o nome de target passado via CLI (ex.: {@code --target kotlin}) para um {@link CodeGenerator}. */
public class GeneratorFactory {

    /** @param target {@code "kotlin"} ou {@code "typescript"}. */
    public static CodeGenerator create(String target) {
        switch (target) {
            case "kotlin":     return new KotlinSpringGenerator();
            case "typescript": return new TypeScriptNestGenerator();
            default:
                throw new IllegalArgumentException("Target inválido: '" + target + "'. Use 'kotlin' ou 'typescript'.");
        }
    }
}
