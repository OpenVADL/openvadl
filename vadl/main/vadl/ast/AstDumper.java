package vadl.ast;

import java.util.Arrays;
import java.util.List;
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
      } else if (child == null) {
        builder.append(indentString()).append("null");
      } else {
        throw new RuntimeException("NOT IMPLEMENTED");
      }
    }
    indent--;
  }

  private void dumpChildren(Node... children) {
    dumpChildren(Arrays.asList(children));
  }

  @Override
  public Void visit(ConstantDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.identifier());
    if (definition.type != null) {
      dumpChildren(definition.type);
    }
    dumpChildren(definition.value);
    return null;
  }

  @Override
  public Void visit(FormatDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.identifier(), definition.type);
    this.indent++;
    for (var field : definition.fields) {
      if (field instanceof FormatDefinition.RangeFormatField f) {
        dumpNode(f);
        dumpChildren(f.identifier);
        dumpChildren(f.ranges);
      } else if (field instanceof FormatDefinition.TypedFormatField f) {
        dumpNode(f);
        dumpChildren(f.identifier, f.type());
      } else if (field instanceof FormatDefinition.DerivedFormatField f) {
        dumpNode(f);
        dumpChildren(f.identifier, f.expr);
      }
    }
    this.indent--;
    return null;
  }

  @Override
  public Void visit(InstructionSetDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.identifier);
    if (definition.extending != null) {
      dumpChildren(definition.extending);
    }
    dumpChildren(definition.definitions);
    return null;
  }

  @Override
  public Void visit(CounterDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.identifier(), definition.type);
    return null;
  }

  @Override
  public Void visit(MemoryDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.identifier(), definition.addressType, definition.dataType);
    return null;
  }

  @Override
  public Void visit(RegisterDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.identifier(), definition.type);
    return null;
  }

  @Override
  public Void visit(RegisterFileDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.identifier(), definition.indexType, definition.registerType);
    return null;
  }

  @Override
  public Void visit(Identifier expr) {
    dumpNode(expr);
    return null;
  }

  @Override
  public Void visit(BinaryExpr expr) {
    dumpNode(expr);
    dumpChildren(expr.left, (Node) expr.operator, expr.right);
    return null;
  }

  @Override
  public Void visit(GroupedExpr expr) {
    dumpNode(expr);
    dumpChildren(expr.expressions);
    return null;
  }

  @Override
  public Void visit(IntegerLiteral expr) {
    dumpNode(expr);
    return null;
  }

  @Override
  public Void visit(BinaryLiteral expr) {
    dumpNode(expr);
    return null;
  }

  @Override
  public Void visit(BoolLiteral expr) {
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
    dumpChildren((Expr) expr.baseType);
    indent++;
    for (List<Expr> sizes : expr.sizeIndices) {
      builder.append(indentString()).append("Sizes\n");
      dumpChildren(sizes);
    }
    indent--;
    return null;
  }

  @Override
  public Void visit(IdentifierPath expr) {
    dumpNode(expr);
    dumpChildren(expr.segments.stream().map(Node.class::cast).toList());
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
    dumpChildren(definition.id(), definition.type(), definition.behavior);
    return null;
  }

  @Override
  public Void visit(EncodingDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.instrId());
    dumpChildren(definition.fieldEncodings().encodings.stream()
        .map(EncodingDefinition.FieldEncoding.class::cast)
        .flatMap(entry -> Stream.of(entry.field(), (Node) entry.value()))
        .toList()
    );
    return null;
  }

  @Override
  public Void visit(AssemblyDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.expr);
    return null;
  }

  @Override
  public Void visit(UsingDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.identifier(), definition.type);
    return null;
  }

  @Override
  public Void visit(FunctionDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.name());
    for (var param : definition.params) {
      dumpChildren(param.name(), param.type());
    }
    dumpChildren(definition.retType, definition.expr);
    return null;
  }

  @Override
  public Void visit(AliasDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.id());
    if (definition.aliasType != null) {
      dumpChildren(definition.aliasType);
    }
    if (definition.targetType != null) {
      dumpChildren(definition.targetType);
    }
    dumpChildren(definition.value);
    return null;
  }

  @Override
  public Void visit(EnumerationDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.id());
    if (definition.enumType != null) {
      dumpChildren(definition.enumType);
    }
    for (var entry : definition.entries) {
      dumpChildren(entry.name());
      if (entry.value() != null) {
        dumpChildren(entry.value());
      }
      if (entry.behavior() != null) {
        dumpChildren(entry.behavior());
      }
    }
    return null;
  }

  @Override
  public Void visit(ExceptionDefinition definition) {
    dumpNode(definition);
    dumpChildren(definition.id());
    dumpChildren(definition.statement);
    return null;
  }

  @Override
  public Void visit(PlaceholderDefinition definition) {
    dumpNode(definition);
    return null;
  }

  @Override
  public Void visit(MacroInstanceDefinition definition) {
    dumpNode(definition);
    return null;
  }

  @Override
  public Void visit(MacroMatchDefinition definition) {
    dumpNode(definition);
    return null;
  }

  @Override
  public Void visit(CallExpr expr) {
    dumpNode(expr);
    dumpChildren((Expr) expr.target);
    indent++;
    for (List<Expr> args : expr.argsIndices) {
      builder.append(indentString()).append("ArgsIndices\n");
      dumpChildren(args);
    }
    for (CallExpr.SubCall subCall : expr.subCalls) {
      builder.append(indentString()).append("SubCall\n");
      dumpChildren(subCall.id());
      for (List<Expr> args : subCall.argsIndices()) {
        builder.append(indentString()).append("ArgsIndices\n");
        dumpChildren(args);
      }
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
    dumpChildren(expr.identifiers);
    dumpChildren(expr.valueExpr, expr.body);
    return null;
  }

  @Override
  public Void visit(CastExpr expr) {
    dumpNode(expr);
    dumpChildren(expr.value, (Expr) expr.type);
    return null;
  }

  @Override
  public Void visit(SymbolExpr expr) {
    dumpNode(expr);
    dumpChildren((Expr) expr.path);
    if (expr.size != null) {
      dumpChildren(expr.size);
    }
    return null;
  }

  @Override
  public Void visit(OperatorExpr expr) {
    dumpNode(expr);
    return null;
  }

  @Override
  public Void visit(MacroMatchExpr expr) {
    dumpNode(expr);
    return null;
  }

  @Override
  public Void visit(MatchExpr expr) {
    dumpNode(expr);
    dumpChildren(expr.candidate, expr.defaultResult);
    for (var matchCase : expr.cases) {
      dumpChildren(matchCase.patterns());
      dumpChildren(matchCase.result());
    }
    return null;
  }

  @Override
  public Void visit(ExtendIdExpr expr) {
    dumpNode(expr);
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
    assignmentStatement.target.accept(this);
    assignmentStatement.valueExpression.accept(this);
    indent--;
    return null;
  }

  @Override
  public Void visit(RaiseStatement raiseStatement) {
    dumpNode(raiseStatement);
    dumpChildren(raiseStatement.statement);
    return null;
  }

  @Override
  public Void visit(CallStatement callStatement) {
    dumpNode(callStatement);
    dumpChildren(callStatement.expr);
    return null;
  }

  @Override
  public Void visit(PlaceholderStatement placeholderStatement) {
    dumpNode(placeholderStatement);
    return null;
  }

  @Override
  public Void visit(MacroInstanceStatement macroInstanceStatement) {
    dumpNode(macroInstanceStatement);
    return null;
  }

  @Override
  public Void visit(MacroMatchStatement macroMatchStatement) {
    dumpNode(macroMatchStatement);
    return null;
  }

  @Override
  public Void visit(MatchStatement matchStatement) {
    dumpNode(matchStatement);
    dumpChildren(matchStatement.candidate, matchStatement.defaultResult);
    for (var matchCase : matchStatement.cases) {
      dumpChildren(matchCase.patterns());
      dumpChildren(matchCase.result());
    }
    return null;
  }
}
