package br.ufscar.dc.compiladores.apidraft;

import br.ufscar.dc.compiladores.apidraft.ast.ProgramNode;
import br.ufscar.dc.compiladores.apidraft.codegen.CodeGenerator;
import br.ufscar.dc.compiladores.apidraft.codegen.GeneratorFactory;
import br.ufscar.dc.compiladores.apidraft.semantic.SemanticAnalyzer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Ponto de entrada da CLI. Orquestra as quatro fases do compilador em sequência
 * (léxica/sintática -> AST -> semântica -> geração de código), interrompendo com
 * código de saída 1 assim que uma fase reporta erro.
 */
public class Principal {
    private static final String USO =
        "Uso: java -jar api-draft-compiler.jar --target <kotlin|typescript> --output <dir> <arquivo.apid>";

    public static void main(String[] args) {
        if (!argumentosValidos(args)) {
            System.out.println(USO);
            System.exit(1);
        }

        String target = args[1];
        String outputDir = args[3];
        String inputFile = args[4];

        ProgramNode program = parse(inputFile);
        if (program == null) {
            System.exit(1);
        }

        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        List<String> errors = analyzer.analyze(program);
        if (!errors.isEmpty()) {
            errors.forEach(System.err::println);
            System.exit(1);
        }

        try {
            CodeGenerator generator = GeneratorFactory.create(target);
            generator.generate(program, Path.of(outputDir));
        } catch (IOException e) {
            System.err.println("Erro ao gerar arquivos: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Executa léxico + sintático sobre {@code inputFile} e devolve a AST.
     * Erros léxicos são coletados manualmente varrendo os tokens (em vez de só
     * escutar o parser) porque tokens de erro como {@code ERRO_SIMBOLO} e
     * {@code UNCLOSED_PATH} não disparam o listener de sintaxe do ANTLR por si só.
     * Retorna {@code null} e já imprime as mensagens de erro quando há falha.
     */
    public static ProgramNode parse(String inputFile) {
        CharStream cs;
        try {
            cs = CharStreams.fromFileName(inputFile);
        } catch (IOException e) {
            System.err.println("Erro ao abrir arquivo: " + inputFile);
            return null;
        }

        ApiDraftLexer lexer = new ApiDraftLexer(cs);
        CustomErrorListener errorListener = new CustomErrorListener();

        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        tokens.fill();

        for (Token token : tokens.getTokens()) {
            if (token.getType() == ApiDraftLexer.ERRO_SIMBOLO) {
                errorListener.lexicalError(token);
            } else if (token.getType() == ApiDraftLexer.UNCLOSED_PATH) {
                errorListener.unclosedPath(token);
            }
        }

        if (errorListener.hasErrors()) {
            errorListener.getErrors().forEach(System.err::println);
            return null;
        }

        ApiDraftParser parser = new ApiDraftParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        ApiDraftParser.ProgramaContext tree = parser.programa();

        if (errorListener.hasErrors()) {
            errorListener.getErrors().forEach(System.err::println);
            return null;
        }

        AstBuilder builder = new AstBuilder();
        return builder.visitPrograma(tree);
    }

    private static boolean argumentosValidos(String[] args) {
        if (args.length != 5) return false;
        if (!args[0].equals("--target")) return false;
        if (!args[1].equals("kotlin") && !args[1].equals("typescript")) return false;
        if (!args[2].equals("--output")) return false;
        return true;
    }
}
