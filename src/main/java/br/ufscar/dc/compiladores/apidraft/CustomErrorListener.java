package br.ufscar.dc.compiladores.apidraft;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.List;

public class CustomErrorListener extends BaseErrorListener {
    private final List<String> errors = new ArrayList<>();

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                            int line, int charPositionInLine, String msg, RecognitionException e) {
        if (offendingSymbol instanceof Token) {
            Token token = (Token) offendingSymbol;
            String tokenName = ApiDraftLexer.VOCABULARY.getSymbolicName(token.getType());

            if ("ERRO_SIMBOLO".equals(tokenName)) {
                errors.add(String.format(
                    "Erro léxico na linha %d, coluna %d: símbolo '%s' não reconhecido",
                    line, charPositionInLine, token.getText()));
                return;
            }
        }

        String found = offendingSymbol instanceof Token
            ? "'" + ((Token) offendingSymbol).getText() + "'"
            : "desconhecido";
        String expected = extractExpected(msg);

        errors.add(String.format(
            "Erro sintático na linha %d, coluna %d: esperado %s mas encontrado %s",
            line, charPositionInLine, expected, found));
    }

    private String extractExpected(String antlrMsg) {
        // ANTLR emite mensagens como "missing X at Y" ou "mismatched input X expecting Y"
        if (antlrMsg.contains("expecting")) {
            int idx = antlrMsg.indexOf("expecting");
            return antlrMsg.substring(idx + "expecting".length()).trim();
        }
        if (antlrMsg.contains("missing")) {
            int start = antlrMsg.indexOf("missing") + "missing".length();
            int end = antlrMsg.contains(" at ") ? antlrMsg.indexOf(" at ") : antlrMsg.length();
            return antlrMsg.substring(start, end).trim();
        }
        return "token válido";
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public void lexicalError(Token token) {
        errors.add(String.format(
            "Erro léxico na linha %d, coluna %d: símbolo '%s' não reconhecido",
            token.getLine(), token.getCharPositionInLine(), token.getText()));
    }

    public void unclosedPath(Token token) {
        errors.add(String.format(
            "Erro léxico na linha %d, coluna %d: caminho HTTP não fechado com aspas",
            token.getLine(), token.getCharPositionInLine()));
    }

    public List<String> getErrors() {
        return errors;
    }
}
