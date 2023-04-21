import java.util.List;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    private Environment environment = new Environment();

    void interpret(List<Stmt> statements) {
        try {
            statements.forEach(this::execute);
        } catch (RuntimeError error) {
            Main.runtimeError(error);
        }
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        environment.assign(expr.name, value);
        return value;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expression) {
        Object lhs = evaluate(expression.lhs);
        Object rhs = evaluate(expression.rhs);

        return switch(expression.operator.type) {
            case MINUS -> checkNumberOperand(expression.operator, lhs) - checkNumberOperand(expression.operator, rhs);
            case SLASH -> checkNumberOperand(expression.operator, lhs) / checkNumberOperand(expression.operator, rhs);
            case STAR -> checkNumberOperand(expression.operator, lhs) * checkNumberOperand(expression.operator, rhs);
            case PLUS -> {
                if (lhs instanceof Double && rhs instanceof Double) {
                    yield checkNumberOperand(expression.operator, lhs) + checkNumberOperand(expression.operator, rhs);
                }

                if (lhs instanceof String && rhs instanceof String) {
                    yield lhs + (String)rhs;
                }

                yield null;
            }
            case GREATER -> checkNumberOperand(expression.operator, lhs) > checkNumberOperand(expression.operator, rhs);
            case GREATER_EQUAL -> checkNumberOperand(expression.operator, lhs) >= checkNumberOperand(expression.operator, rhs);
            case LESS -> checkNumberOperand(expression.operator, lhs) < checkNumberOperand(expression.operator, rhs);
            case LESS_EQUAL -> checkNumberOperand(expression.operator, lhs) <= checkNumberOperand(expression.operator, rhs);
            case BANG_EQUAL -> !isEqual(lhs, rhs);
            case EQUAL_EQUAL -> isEqual(lhs, rhs);
            default -> null;
        };
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object rhs = evaluate(expr.rhs);
        return switch (expr.operator.type) {
            case MINUS -> -checkNumberOperand(expr.operator, rhs);
            case BANG -> !isTruthy(rhs);
            default -> null;
        };
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.name);
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        System.out.println(stringify(evaluate(stmt.expression)));
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        environment.define(stmt.name.lexeme, stmt.initializer != null ? evaluate(stmt.initializer) : null);
        return null;
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    private void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;
            statements.forEach(this::execute);
        } finally {
            this.environment = previous;
        }
    }

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean)object;
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;
        return a.equals(b);
    }

    private double checkNumberOperand(Token operator, Object operand) {
        if (!(operand instanceof Double)) throw new RuntimeError(operator, "Operand must be a number.");
        return (double) operand;
    }

    private String stringify(Object object) {
        if (object == null) return "nil";

        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return object.toString();
    }


}
