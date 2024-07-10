package vadl.ast;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * A pass over the AST that produces a textual representation of the AST.
 */
public class AstDumper implements DefinitionVisitor<Void>, ExprVisitor<Void> {
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
      if (child instanceof Definition) {
        ((Definition) child).accept(this);
      } else if (child instanceof Expr) {
        ((Expr) child).accept(this);
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
      dumpNode(field);
      dumpChildren(field.identifier);
      dumpChildren(field.ranges);
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
  public Void visit(PlaceHolderExpr expr) {
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
  public Void visit(Variable expr) {
    dumpNode(expr);
    dumpChildren(expr.identifier);
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
    dumpChildren(definition.identifier, definition.typeIdentifier /*, definition.block */);
    return null;
  }

  @Override
  public Void visit(EncodingDefinition definition) {
    dumpNode(definition);
    Stream<Node> entries = definition.entries.stream()
        .flatMap(entry -> Stream.of(entry.field(), entry.value()));
    Stream<Node> children = Stream.concat(Stream.of(definition.instrIdentifier), entries);
    dumpChildren(children.toArray(Node[]::new));
    return null;
  }
}
