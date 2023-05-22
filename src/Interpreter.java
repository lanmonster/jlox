import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    final Environment globals = new Environment();

    private final Map<Expr, Integer> locals = new HashMap<>();

    private Environment environment = globals;

    Interpreter() {
        globals.define("clock", new LoxCallable() {
            @Override
            public int arity() { return 0; }

            @Override
            public Object call(Interpreter interpreter,List<Object> arguments) {
                return (double)System.currentTimeMillis() / 1000.0;
            }

            @Override
            public String toString() { return "<native fn>"; }
        });
    }


    void interpret(List<Stmt> statements) {
        try {
            statements.forEach(this::execute);
        } catch (RuntimeError error) {
            Main.runtimeError(error);
        }
    }

    private Object lookUpVariable(Token name, Expr expr) {
        Integer distance = locals.get(expr);
        if (distance != null) {
            return environment.getAt(distance, name.lexeme);
        } else {
            return globals.get(name);
        }
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        Integer distance = locals.get(expr);
        if (distance != null) {
            environment.assignAt(distance, expr.name, value);
        } else {
            globals.assign(expr.name, value);
        }
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
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);
        if (!(callee instanceof LoxCallable function)) {
            throw new RuntimeError(expr.paren, "Can only call functions and classes.");
        }
        List<Object> arguments = expr.arguments.stream().map(this::evaluate).toList();
        if (arguments.size() != function.arity()) {
            throw new RuntimeError(expr.paren, "Expected " +
                    function.arity() + " arguments but got " +
                    arguments.size() + ".");
        }
        return function.call(this, arguments);
    }

    @Override
    public Object visitGetExpr(Expr.Get expr) {
        Object object = evaluate(expr.object);
        if (object instanceof LoxInstance instance) {
            return instance.get(expr.name);
        }
        throw new RuntimeError(expr.name, "Only instances have properties.");
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
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.lhs);

        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left)) return left;
        } else {
            if (!isTruthy(left)) return left;
        }

        return evaluate(expr.rhs);
    }

    @Override
    public Object visitSetExpr(Expr.Set expr) {
        Object object = evaluate(expr.object);

        if (!(object instanceof LoxInstance)) {
            throw new RuntimeError(expr.name, "Only instances have fields.");
        }

        Object value = evaluate(expr.value);
        ((LoxInstance)object).set(expr.name, value);
        return value;
    }

    @Override
    public Object visitThisExpr(Expr.This expr) {
        return lookUpVariable(expr.keyword, expr);
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
        return lookUpVariable(expr.name, expr);
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        environment.define(stmt.name.lexeme, null);
        Map<String, LoxFunction> methods = new HashMap<>();
        for (Stmt.Function method : stmt.methods) {
            LoxFunction function = new LoxFunction(method, environment, method.name.lexeme.equals("init"));
            methods.put(method.name.lexeme, function);
        }

        LoxClass klass = new LoxClass(stmt.name.lexeme, methods);
        environment.assign(stmt.name, klass);

        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        LoxFunction function = new LoxFunction(stmt, environment, false);
        environment.define(stmt.name.lexeme, function);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        System.out.println(stringify(evaluate(stmt.expression)));
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null) value = evaluate(stmt.value);

        throw new Return(value);
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        environment.define(stmt.name.lexeme, stmt.initializer != null ? evaluate(stmt.initializer) : null);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (isTruthy(stmt.condition)) {
            execute(stmt.body);
        }
        return null;
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    void resolve(Expr expr, int depth) {
        locals.put(expr, depth);
    }

    protected void executeBlock(List<Stmt> statements, Environment environment) {
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
