package br.ufscar.dc.compiladores.apidraft;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;

import java.io.PrintWriter;

public class CustomErrorListener extends BaseErrorListener {
    private final PrintWriter pw;
    private boolean errorFound = false;

    public CustomErrorListener(PrintWriter pw) {
        this.pw = pw;
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                            int line, int charPositionInLine, String msg, RecognitionException e) {
        if (errorFound) return;

        String text = "<EOF>";
        if (offendingSymbol instanceof Token) {
            Token t = (Token) offendingSymbol;
            text = t.getText().equals("<EOF>") ? "EOF" : t.getText();
        }

        pw.println("Linha " + line + ":" + charPositionInLine + ": erro sintatico proximo a " + text);
        errorFound = true;

        throw new RuntimeException("ParseError");
    }

    public boolean hasError() {
        return errorFound;
    }
}
