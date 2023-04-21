class ASTPrinter implements Expr.Visitor<String> {
    public static void main(String[] args) {
        Expr expression = new Expr.Binary(
                new Expr.Unary(
                        new Token(TokenType.MINUS, "-", null, 1),
                        new Expr.Literal(123)
                ),
                new Token(TokenType.STAR, "*", null, 1),
                new Expr.Grouping(new Expr.Literal(45.67)));

        System.out.println(new ASTPrinter().print(expression));
    }

    String print(Expr expr) {
        return expr.accept(this);
    }

    private String parenthesize(String name, Expr... expressions) {
        StringBuilder builder = new StringBuilder();

        builder.append("(").append(name);
        for (Expr expr : expressions) {
            builder.append(" ");
            builder.append(expr.accept(this));
        }
        builder.append(")");

        return builder.toString();
    }

    @Override
    public String visitAssignExpr(Expr.Assign expr) {
        return parenthesize("SET " + expr.name.lexeme + " to ", expr.value);
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expression) {
        return parenthesize(expression.operator.lexeme, expression.lhs, expression.rhs);
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expression) {
        return parenthesize("group", expression.expression);
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expression) {
        return expression.value == null ? "nil" : expression.value.toString();
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expression) {
        return parenthesize(expression.operator.lexeme, expression.rhs);
    }

    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        return parenthesize(expr.name.lexeme);
    }
}