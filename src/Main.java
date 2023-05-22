import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

// Press ⇧ twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
public class Main {
    static boolean hadError = false;

    static boolean hadRuntimeError = false;
    private static final Interpreter interpreter = new Interpreter();


    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("usage: jlox <script>");
            System.exit(64);
        }

        if (args.length == 1) {
            run(Files.readString(Paths.get(args[0]), Charset.defaultCharset()));
            if (hadError) System.exit(65);
            if (hadRuntimeError) System.exit(70);
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        for (;;) {
            System.out.print("> ");
            String line = reader.readLine();
            if (line == null) break;
            run(line);
            hadError = false;
        }
    }

    private static void run(String source) {
        Parser parser = new Parser(new Lexer(source).lex());
        List<Stmt> statements = parser.parse();

        Resolver resolver = new Resolver(interpreter);
        resolver.resolve(statements);

        // Stop if there was a syntax error.
        if (hadError) return;

        interpreter.interpret(statements);

        if (hadRuntimeError) System.exit(70);
    }

    static void error(int line, String message) {
        report(line, "", message);
    }

    private static void report(int line, String where, String message) {
        System.err.println("[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }

    static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
    }

    static void runtimeError(RuntimeError error) {
        System.err.println(error.getMessage() + "\n[line " + error.token.line + "]");
        hadRuntimeError = true;
    }
}