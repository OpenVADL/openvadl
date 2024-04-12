package vadl.ast;

/**
 * The Expression node of the AST.
 */
public abstract class Expr extends Node {
}

class IntegerLiteral extends Expr {
  long number;
  Location loc;

  public IntegerLiteral(long number, Location loc) {
    this.number = number;
    this.loc = loc;
  }

  @Override
  Location location() {
    return loc;
  }

  @Override
  void dump(int indent, StringBuilder builder) {
    builder.append(indentString(indent));
    builder.append("IntegerLiteral (value: %d)\n".formatted(number));
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(number);
  }
}

/**
 * Any kind of binary expression (often written with the infix notation in vadl).
 */
class BinaryExpr extends Expr {
  Expr left;
  Operation operation;
  Expr right;

  BinaryExpr(Expr left, Operation operation, Expr right) {
    this.left = left;
    this.operation = operation;
    this.right = right;
  }

  String operationAsString(Operation op) {
    return switch (op) {
      case ADD -> "+";
      case SUBTRACT -> "-";
      case MULTIPLY -> "*";
      case DIVIDE -> "/";
    };
  }

  @Override
  Location location() {
    return new Location(left.location(), right.location());
  }

  @Override
  void dump(int indent, StringBuilder builder) {
    builder.append(indentString(indent));
    builder.append("BinaryExpr (operation: %s)\n".formatted(operationAsString(operation)));
    left.dump(indent + 1, builder);
    right.dump(indent + 1, builder);
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    left.prettyPrint(indent, builder);
    builder.append(" %s ".formatted(operationAsString(operation)));
    right.prettyPrint(indent, builder);
  }

  enum Operation {
    ADD,
    SUBTRACT,
    MULTIPLY,
    DIVIDE,
  }
}

