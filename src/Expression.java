import java.util.List;

abstract class Expression {
  interface Visitor<R> {
    R visitBinaryExpression(Binary expression);
    R visitGroupingExpression(Grouping expression);
    R visitLiteralExpression(Literal expression);
    R visitUnaryExpression(Unary expression);
  }
  static class Binary extends Expression {
    Binary(Expression lhs, Token operator, Expression rhs) {
      this.lhs = lhs;
      this.operator = operator;
      this.rhs = rhs;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitBinaryExpression(this);
    }

    final Expression lhs;
    final Token operator;
    final Expression rhs;
  }
  static class Grouping extends Expression {
    Grouping(Expression expression) {
      this.expression = expression;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitGroupingExpression(this);
    }

    final Expression expression;
  }
  static class Literal extends Expression {
    Literal(Object value) {
      this.value = value;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitLiteralExpression(this);
    }

    final Object value;
  }
  static class Unary extends Expression {
    Unary(Token operator, Expression rhs) {
      this.operator = operator;
      this.rhs = rhs;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitUnaryExpression(this);
    }

    final Token operator;
    final Expression rhs;
  }

  abstract <R> R accept(Visitor<R> visitor);
}
