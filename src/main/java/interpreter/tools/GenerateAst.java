package interpreter.tools;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class GenerateAst {
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("usage: generate_ast <output directory>");
            System.exit(64);
        }
        String outputDir = args[0];
        defineAst(outputDir, "Expr", Arrays.asList(
                "Assign   : Token name, Expr value",
                "Variable : Token name",
                "Ternary  : Expr leftHand, Expr left, Expr right",
                "Logical  : Expr left, Token operator, Expr right",
                "Set      : Expr object, Token name, Expr value",
                "Super    : Token keyword, Token method",
                "This     : Token keyword",
                "Binary   : Expr left, Token operator, Expr right",
                "Unary    : Token operator, Expr right",
                "Literal  : Object value",
                "Call     : Expr callee, Token paren, List<Expr> arguments ",
                "Get      : Expr object, Token name",
                "Grouping : Expr expression",
                "AnFunction : List<Token> parameters, List<Stmt> body"
        ));
        defineAst(outputDir, "Stmt", Arrays.asList(
                "Class      : Token name, Expr.Variable superclass , List<Stmt.Function> methods",
                "Var        : Token name, Expr initializer",
                "Expression : Expr expression",
                "Function   : Token name, Expr function, String kind",
                "If         : Expr condition, Stmt thenBranch, Stmt elseBranch",
                "While      : Expr condition, Stmt body",
                "Print      : Expr expression",
                "Block      : List<Stmt> statements",
                "Return     : Token keyWord, Expr value",
                "Break      : Token name",
                "Import     : Token name, Token alias"
        ));
    }
    private static void defineAst(String outputDir, String baseName, List<String> types) throws IOException {
        String path = outputDir + "/" + baseName + ".java";
        PrintWriter writer = new PrintWriter(path, "UTF-8");

        writer.println("package interpreter.lox;");

        writer.println();
        writer.println("import java.util.List;");
        writer.println();
        writer.println("abstract class " + baseName + " {");

        defineVisitor(writer, baseName, types);
        writer.println();
        writer.println("    abstract <R> R accept(Visitor<R> visitor);");
        writer.println();

        for (String type : types) {
            String className = type.split(":")[0].trim();
            String fields = type.split(":")[1].trim();
            defineType(writer, baseName, className, fields);
        }

        writer.println("}");
        writer.close();
    }

    private static void defineVisitor(PrintWriter writer, String baseName, List<String> types) {
        writer.println("    interface Visitor<R> {");

        for (String type : types) {
            String typeName = type.split(":")[0].trim();
            writer.println("        R visit" + typeName + baseName + "(" +
                    typeName + " " + baseName.toLowerCase() + ");");
        }
        writer.println("    }");
    }

    private static void defineType(PrintWriter writer, String baseName, String className, String fieldsList) {
        writer.println("    static class " + className + " extends " + baseName + " {");

        String[] fields = fieldsList.split(", ");

        // fields
        for (String field : fields) {
            writer.println("        final " + field + ";");
        }
        writer.println();

        // constructor
        writer.println("        public " + className + "(" + fieldsList + ") {");

        // store parameters in fields
        for (String field : fields) {
            String name = field.split(" ")[1];
            writer.println("            this." + name + " = " + name + ";");
        }
        writer.println("        }"); // end of constructor

        // visitor pattern
        writer.println();
        writer.println("        @Override");
        writer.println("        <R> R accept(Visitor<R> visitor) {");
        writer.println("            return visitor.visit" + className + baseName + "(this);");
        writer.println("        }");

        writer.println("    }");
    }
}
