package vadl.ast;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A pass over the AST that produces a textual representation of the AST.
 */
public class AstDumper
    implements DefinitionVisitor<Void>, ExprVisitor<Void>, StatementVisitor<Void> {
  private StringBuilder builder = new StringBuilder();
  private int indent;

  /**
   * Dumps the AST into a textual representation.
   *
   * @param ast to dump.
   * @return a textual representation of the tree.
   */
  public String dump(Ast ast) {
    builder = new StringBuilder();
    indent = 0;

    for (var definition : ast.definitions) {
      definition.accept(this);
    }
    return builder.toString();
  }

  private String indentString() {
    var indentBy = 2;
    var indentCharacters = ". : ' | ";
    var indentLength = indent * indentBy;
    return indentCharacters.repeat(indentLength / indentCharacters.length())
        + indentCharacters.substring(0, indentLength % indentCharacters.length());
  }

  private void dumpNode(Node node) {
    builder.append(indentString());
    builder.append(node.toString());
    builder.append('\n');
  }

  private void dumpChildren(List<? extends Node> children) {
    indent++;
    for (var child : children) {
      if (child instanceof Definition def) {
        def.accept(this);
      } else if (child instanceof Expr expr) {
        expr.accept(this);
      } else if (child instanceof Statement statement) {
        statement.accept(this);
      } else if (child instanceof Identifier) {
        dumpNode(child);
      } else {
        System.out.println(child);
        throw new RuntimeException("NOT IMPLEMENTED");
      }
    }
    indent--;
  }

  private void dumpChildren(Node... children) {
    dumpChildren(Arrays.stream(children).toList());
  }

  @Override
  public Void visit(ConstantDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.identifier);
    if (definition.typeAnnotation != null) {
      dumpChildren(definition.typeAnnotation);
    }
    dumpChildren(definition.value);
    return null;
  }

  @Override
  public Void visit(FormatDefinition definition) {
    dumpNode(definition);
    this.indent++;
    for (var field : definition.fields) {
      if (field instanceof FormatDefinition.RangeFormatField f) {
        dumpNode(f);
        dumpChildren(f.identifier());
        dumpChildren(f.ranges);
      } else if (field instanceof FormatDefinition.TypedFormatField f) {
        dumpNode(f);
        dumpChildren(f.identifier());
        dumpChildren(f.typeAnnotation);
      }
    }
    this.indent--;
    return null;
  }

  @Override
  public Void visit(InstructionSetDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.identifier);
    dumpChildren(definition.definitions.toArray(new Node[0]));
    return null;
  }

  @Override
  public Void visit(CounterDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.identifier, definition.type);
    return null;
  }

  @Override
  public Void visit(MemoryDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.identifier, definition.addressType, definition.dataType);
    return null;
  }

  @Override
  public Void visit(RegisterDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.identifier, definition.type);
    return null;
  }

  @Override
  public Void visit(RegisterFileDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.identifier, definition.indexType, definition.registerType);
    return null;
  }

  @Override
  public Void visit(BinaryExpr expr) {
    dumpNode(expr);
    dumpChildren(expr.left, expr.right);
    return null;
  }

  @Override
  public Void visit(GroupExpr expr) {
    dumpNode(expr);
    dumpChildren(expr.inner);
    return null;
  }

  @Override
  public Void visit(IntegerLiteral expr) {
    dumpNode(expr);
    return null;
  }

  @Override
  public Void visit(StringLiteral expr) {
    dumpNode(expr);
    return null;
  }

  @Override
  public Void visit(PlaceholderExpr expr) {
    dumpNode(expr);
    return null;
  }

  @Override
  public Void visit(MacroInstanceExpr expr) {
    dumpNode(expr);
    return null;
  }

  @Override
  public Void visit(RangeExpr expr) {
    dumpNode(expr);
    dumpChildren(expr.from, expr.to);
    return null;
  }

  @Override
  public Void visit(TypeLiteral expr) {
    dumpNode(expr);
    dumpChildren(expr.baseType);
    if (expr.sizeExpression != null) {
      dumpChildren(expr.sizeExpression);
    }
    return null;
  }

  @Override
  public Void visit(IdentifierChain expr) {
    dumpNode(expr);
    dumpChildren(expr.identifier);
    if (expr.next != null) {
      dumpChildren(expr.next);
    }
    return null;
  }

  @Override
  public Void visit(UnaryExpr expr) {
    dumpNode(expr);
    dumpChildren(expr.operand);
    return null;
  }

  @Override
  public Void visit(InstructionDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.identifier, definition.typeIdentifier, definition.behavior);
    return null;
  }

  @Override
  public Void visit(EncodingDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.instrIdentifier);
    dumpChildren(definition.fieldEncodings.stream()
        .flatMap(entry -> Stream.of(entry.field(), entry.value()))
        .toList()
    );
    return null;
  }

  @Override
  public Void visit(AssemblyDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.segments);
    return null;
  }

  @Override
  public Void visit(CallExpr expr) {
    dumpNode(expr);
    dumpChildren(expr.target);
    indent++;
    for (List<Expr> invocation : expr.invocations) {
      builder.append(indentString()).append("INVOCATION\n");
      dumpChildren(invocation);
    }
    indent--;
    return null;
  }

  @Override
  public Void visit(IfExpr expr) {
    dumpNode(expr);
    dumpChildren(expr.condition, expr.thenExpr, expr.elseExpr);
    return null;
  }

  @Override
  public Void visit(LetExpr expr) {
    dumpNode(expr);
    dumpChildren(expr.identifier, expr.valueExpr, expr.body);
    return null;
  }

  @Override
  public Void visit(CastExpr expr) {
    dumpNode(expr);
    dumpChildren(expr.value, expr.type);
    return null;
  }

  @Override
  public Void visit(SymbolExpr expr) {
    dumpNode(expr);
    dumpChildren(expr.target);
    if (expr.size != null) {
      dumpChildren(expr.size);
    }
    return null;
  }

  @Override
  public Void visit(BlockStatement blockStatement) {
    dumpNode(blockStatement);
    dumpChildren(blockStatement.statements);
    return null;
  }

  @Override
  public Void visit(LetStatement letStatement) {
    dumpNode(letStatement);
    dumpChildren(letStatement.valueExpression, letStatement.body);
    return null;
  }

  @Override
  public Void visit(IfStatement ifStatement) {
    dumpNode(ifStatement);
    dumpChildren(ifStatement.condition, ifStatement.thenStmt);
    if (ifStatement.elseStmt != null) {
      dumpChildren(ifStatement.elseStmt);
    }
    return null;
  }

  @Override
  public Void visit(AssignmentStatement assignmentStatement) {
    builder.append(indentString()).append("AssignmentStatement\n");
    indent++;
    builder.append(indentString()).append("Target:\n");
    assignmentStatement.target.accept(this);
    builder.append(indentString()).append("Value:");
    assignmentStatement.valueExpression.accept(this);
    indent--;
    return null;
  }
}
