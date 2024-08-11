package vadl.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.utils.SourceLocation;

class ParserUtils {

  static boolean[] NO_OPS;
  static boolean[] BIN_OPS;
  static boolean[] BIN_OPS_EXCEPT_GT;
  static boolean[] BIN_OPS_EXCEPT_IN;

  static {
    NO_OPS = new boolean[Parser.maxT + 1];

    BIN_OPS = NO_OPS.clone();
    BIN_OPS[Parser._SYM_LOGOR] = true;
    BIN_OPS[Parser._SYM_LOGAND] = true;
    BIN_OPS[Parser._SYM_BINOR] = true;
    BIN_OPS[Parser._SYM_CARET] = true;
    BIN_OPS[Parser._SYM_BINAND] = true;
    BIN_OPS[Parser._SYM_EQ] = true;
    BIN_OPS[Parser._SYM_NEQ] = true;
    BIN_OPS[Parser._SYM_GTE] = true;
    BIN_OPS[Parser._SYM_GT] = true;
    BIN_OPS[Parser._SYM_LTE] = true;
    BIN_OPS[Parser._SYM_LT] = true;
    BIN_OPS[Parser._SYM_ROTR] = true;
    BIN_OPS[Parser._SYM_ROTL] = true;
    BIN_OPS[Parser._SYM_SHR] = true;
    BIN_OPS[Parser._SYM_SHL] = true;
    BIN_OPS[Parser._SYM_PLUS] = true;
    BIN_OPS[Parser._SYM_MINUS] = true;
    BIN_OPS[Parser._SYM_MUL] = true;
    BIN_OPS[Parser._SYM_DIV] = true;
    BIN_OPS[Parser._SYM_MOD] = true;
    BIN_OPS[Parser._SYM_IN] = true;
    BIN_OPS[Parser._SYM_NIN] = true;

    BIN_OPS_EXCEPT_GT = BIN_OPS.clone();
    BIN_OPS_EXCEPT_GT[Parser._SYM_GT] = false;

    BIN_OPS_EXCEPT_IN = BIN_OPS.clone();
    BIN_OPS_EXCEPT_IN[Parser._SYM_IN] = false;
  }

  /**
   * If the given expression is a binary expression and the parser is not currently parsing a model,
   * it will reorder the expression's operand according to {@link BinaryExpr#reorder(BinaryExpr)}.
   */
  static Expr reorderBinary(Parser parser, Expr expr) {
    // Only if not inside model parsing.
    // Cause there we don't know yet what the order is.
    if (!parser.insideMacro && expr instanceof BinaryExpr binaryExpr) {
      return BinaryExpr.reorder(binaryExpr);
    }
    return expr;
  }

  /**
   * Reorders the tail end of a left-sided binary expression tree "expr" to apply a cast.
   * If the given "symOrBin" is a symbol expression, it will be converted to the type to cast to.
   * If the given "symOrBin" is a binary expression, it has to have a SymbolExpr in its left side,
   * which will be interpreted as the target type.
   * If the given "expr" is not a binary expression, the cast operand will be the whole "expr".
   * If the given "expr" is a binary expression, only its right leaf will be the cast operand.
   *
   * @param expr A left-sided binary expression tree or a non-binary expression.
   * @param symOrBin Either a SymbolExpr of the cast target type, or a BinaryExpr with the
   *                 SymbolExpr as the left operand.
   * @return A left-sided binary expression tree with a CastExpr as its leaf — or a simple CastExpr.
   */
  static Expr reorderCastExpr(Expr expr, Expr symOrBin) {
    Expr castee = expr instanceof BinaryExpr binExpr ? binExpr.right : expr;
    if (castee instanceof GroupExpr groupExpr) {
      castee = groupExpr.inner;
    }
    if (symOrBin instanceof BinaryExpr binSym) {
      var castExpr = new CastExpr(castee, typeLiteral(binSym.left));
      if (expr instanceof BinaryExpr binExpr) {
        binExpr.right = castExpr;
        binSym.left = binExpr;
      } else {
        binSym.left = castExpr;
      }
      return binSym;
    } else {
      var castExpr = new CastExpr(castee, typeLiteral(symOrBin));
      if (expr instanceof BinaryExpr binExpr) {
        binExpr.right = castExpr;
        return binExpr;
      } else {
        return castExpr;
      }
    }
  }

  private static TypeLiteralOrPlaceholder typeLiteral(Expr expr) {
    if (expr instanceof IsSymExpr symExpr) {
      return new TypeLiteral(symExpr);
    } else if (expr instanceof PlaceholderExpr placeholderExpr) {
      return placeholderExpr;
    } else {
      throw new IllegalArgumentException("Unknown type literal node " + expr);
    }
  }

  /**
   * Converts the parser's current token position to a vadl location.
   */
  static SourceLocation locationFromToken(Parser parser, Token token) {
    return new SourceLocation(
        parser.sourceFile,
        new SourceLocation.Position(token.line, token.col),
        new SourceLocation.Position(token.line, token.col + token.val.length()));
  }

  /**
   * Expands a macro.
   */
  static @Nullable Node expandMacro(Parser parser, Identifier identifier, List<Node> args,
                                    SyntaxType requiredReturnType) {
    var unexpanded = new MacroInstanceExpr(identifier, args, identifier.location());
    if (parser.insideMacro) {
      return unexpanded;
    }

    if (BasicSyntaxType.Id().isSubTypeOf(requiredReturnType)
        && parser.macroOverrides.containsKey(identifier.name)) {
      return parser.macroOverrides.get(identifier.name);
    }

    var macro = parser.symbolTable.getMacro(identifier.name);
    if (macro == null) {
      parser.errors.SemErr(parser.t.line, parser.t.col,
          "No macro named `%s` exists.".formatted(identifier.name));
      return unexpanded;
    }

    // The macro itself was invalid but an error for it was already issued,
    // so we silently abort the expansion here.
    if (macro.returnType() == BasicSyntaxType.Invalid()) {
      return unexpanded;
    }

    boolean hasError = false;

    // Verify the arguments
    if (macro.params().size() != args.size()) {
      parser.errors.SemErr(parser.t.line, parser.t.col,
          "The macro `%s` expects %d arguments but %d were provided.".formatted(identifier.name,
              macro.params().size(), args.size()));
      hasError = true;
    }

    var argMap = new HashMap<String, Node>();
    if (!hasError) {
      for (int i = 0; i < args.size(); i++) {
        var arg = args.get(i);
        var param = macro.params().get(i);
        var argType = arg.syntaxType();

        if (!argType.isSubTypeOf(param.type())) {
          parser.errors.SemErr(parser.t.line, parser.t.col,
              ("The macro's `%s` parameter `%s` expects a `%s` but the argument provided is of type"
                  + " `%s`.").formatted(identifier.name, param.name().name, param.type(), argType));
          hasError = true;
        }
        argMap.put(param.name().name, arg);
      }
    }

    // Verify the return type
    if (!macro.returnType().isSubTypeOf(requiredReturnType)) {
      parser.errors.SemErr(parser.t.line, parser.t.col,
          "The macro `%s` returns `%s` but here a macro returning `%s` is expected.".formatted(
              identifier.name, macro.returnType(), requiredReturnType));
      hasError = true;
    }

    if (hasError) {
      return unexpanded;
    }

    // FIXME: There should be a real instantiator here
    var expander = new MacroExpander(argMap, parser.symbolTable);
    var body = macro.body();
    if (body instanceof Expr expr) {
      var expanded = expander.expandExpr(expr);
      return new GroupExpr(expanded);
    } else if (body instanceof DefinitionList definitionList) {
      var items = new ArrayList<Definition>(definitionList.items.size());
      for (Definition item : definitionList.items) {
        Definition definition = expander.expandDefinition(item);
        if (definition instanceof DefinitionList list) {
          items.addAll(list.items);
        } else {
          items.add(definition);
        }
      }
      return new DefinitionList(items, definitionList.location);
    } else if (body instanceof Definition def) {
      return expander.expandDefinition(def);
    } else if (body instanceof StatementList statementList) {
      var items = new ArrayList<Statement>(statementList.items.size());
      for (Statement item : statementList.items) {
        Statement statement = expander.expandStatement(item);
        items.add(statement);
      }
      return new StatementList(items, statementList.location);
    } else if (body instanceof Statement statement) {
      return expander.expandStatement(statement);
    } else {
      throw new RuntimeException("Expanding %s not yet implemented".formatted(body.getClass()));
    }
  }

  static Node narrowNode(Node node) {
    if (node instanceof StatementList statementList) {
      return statementList.items.get(0);
    }
    return node;
  }

  static void pushScope(Parser parser) {
    parser.symbolTable = parser.symbolTable.createChild();
  }

  static void pushFormatScope(Parser parser, IdentifierOrPlaceholder formatId) {
    if (formatId instanceof Identifier id) {
      parser.symbolTable = parser.symbolTable.createFormatScope(id);
    } else {
      pushScope(parser);
    }
  }

  static void pushInstructionScope(Parser parser, IdentifierOrPlaceholder instrId) {
    if (instrId instanceof Identifier id) {
      parser.symbolTable = parser.symbolTable.createInstructionScope(id);
    } else {
      pushScope(parser);
    }
  }

  static void pushFunctionScope(Parser parser, List<FunctionDefinition.Parameter> params) {
    parser.symbolTable = parser.symbolTable.createFunctionScope(params);
  }

  static void popScope(Parser parser) {
    parser.symbolTable = Objects.requireNonNull(parser.symbolTable.parent);
  }

  static boolean isExprType(SyntaxType type) {
    return type.isSubTypeOf(BasicSyntaxType.Ex());
  }

  static boolean isDefType(SyntaxType type) {
    return type.isSubTypeOf(BasicSyntaxType.IsaDefs());
  }
}
