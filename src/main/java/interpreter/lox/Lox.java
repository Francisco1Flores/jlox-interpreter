package interpreter.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static interpreter.lox.TokenType.*;

public class Lox {

    private static final Interpreter interpreter = new Interpreter();

    static boolean hadError = false;
    static boolean hadRuntimeError = false;
    static boolean promptmode = false;

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: jlox [script]");
            System.exit(64);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()), false);
        if (hadError) {
            System.exit(65);
        }
        if (hadRuntimeError) {
            System.exit(70);
        }
    }

    public static List<Object> runModuleFile(String path) throws IOException {
        var find = Files.find(Paths.get("").toAbsolutePath(),
                100,
                (p, f) -> {
                    if (p.getFileName() == null) {
                        return false;
                    }
                    if (p.toString().contains("target")) {
                        return false;
                    }
                    return p.getFileName().toString().equals(path);
                })
                .toList();



        if (find.isEmpty()) {
            throw new IOException("Can't find '" + path + "'.");
        } else if (find.size() > 1) {
            throw new IOException(find.size() + " modules named '"+ path + "' found.");
        }
        byte[] bytes = Files.readAllBytes(find.getFirst());
        var mod = run(new String(bytes, Charset.defaultCharset()), true);
        if (hadError) {
            System.exit(65);
        }
        if (hadRuntimeError) {
            System.exit(70);
        }
        return mod;
    }

    private static void runPrompt() throws IOException {
        promptmode = true;
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);
        for (;;) {
            System.out.print(">>");
            String line = reader.readLine();
            if (line.equals("exit")) {
                System.out.println("Chau puto");
                System.exit(0);
            }
            if (line == null) {
                break;
            }
            run(line, false);
            if (hadError || hadRuntimeError) {
                try {
                    Thread.sleep(500l);
                } catch (InterruptedException e) {
                    System.out.println(e.getMessage());
                }
            }
            hadError = false;
            hadRuntimeError = false;
        }
    }

    private static List<Object> run(String source, boolean mod) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();

        if (hadError) {
            return null;
        }

        Resolver resolver = new Resolver(interpreter);
        resolver.resolve(statements);
        resolver.endScope();

        if (hadError) {
            return null;
        }

        if (mod) {
            return interpreter.interpretModule(statements);
        }
        interpreter.interpret(statements);
        return null;
    }

    static void error(int line, String message) {
        report(line, "", message);
    }

    static void error(Token token, String message) {
        if (token.type == EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
    }

    static void runtimeError(RuntimeError error) {
        System.err.println("[line " + error.token.line + "] Error: " + error.getMessage());
        hadRuntimeError = true;
    }

    private static void report(int line, String where, String message) {
        System.err.println("[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }
}
