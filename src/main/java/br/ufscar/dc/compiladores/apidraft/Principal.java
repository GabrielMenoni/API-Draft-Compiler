package br.ufscar.dc.compiladores.apidraft;

import br.ufscar.dc.compiladores.apidraft.ast.ProgramNode;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.IOException;

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

        // Phases following (US3+): semantic analysis and code generation
        // (code generation not yet implemented)
    }

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
