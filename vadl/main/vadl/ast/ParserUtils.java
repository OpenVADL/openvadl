package vadl.ast;

import java.util.Iterator;
import java.util.List;
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
   * @param expr     A left-sided binary expression tree or a non-binary expression.
   * @param symOrBin Either a SymbolExpr of the cast target type, or a BinaryExpr with the
   *                 SymbolExpr as the left operand.
   * @return A left-sided binary expression tree with a CastExpr as its leaf — or a simple CastExpr.
   */
  static Expr reorderCastExpr(Expr expr, Expr symOrBin) {
    Expr castee = expr instanceof BinaryExpr binExpr ? binExpr.right : expr;
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
    if (expr instanceof PlaceholderExpr placeholderExpr) {
      return placeholderExpr;
    } else if (expr instanceof IsSymExpr symExpr) {
      return new TypeLiteral(symExpr);
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

  static boolean isExprType(SyntaxType type) {
    return type.isSubTypeOf(BasicSyntaxType.EX);
  }

  static boolean isDefType(SyntaxType type) {
    return type.isSubTypeOf(BasicSyntaxType.ISA_DEFS);
  }

  static boolean isStmtType(SyntaxType type) {
    return type.isSubTypeOf(BasicSyntaxType.STATS);
  }

  static MacroOrPlaceholder macroOrPlaceholder(@Nullable Macro macro, SyntaxType syntaxType,
                                               List<String> segments) {
    if (macro != null) {
      return macro;
    }
    return new MacroPlaceholder((ProjectionType) syntaxType, segments);
  }

  static Node createMacroInstance(MacroOrPlaceholder macroOrPlaceholder, List<Node> args,
                                  SourceLocation sourceLocation) {
    if (isDefType(macroOrPlaceholder.returnType())) {
      return new MacroInstanceDefinition(macroOrPlaceholder, args, sourceLocation);
    } else if (isStmtType(macroOrPlaceholder.returnType())) {
      return new MacroInstanceStatement(macroOrPlaceholder, args, sourceLocation);
    } else {
      return new MacroInstanceExpr(macroOrPlaceholder, args, sourceLocation);
    }
  }

  static Node createPlaceholder(SyntaxType type, List<String> path, SourceLocation sourceLocation) {
    if (isDefType(type)) {
      return new PlaceholderDefinition(path, type, sourceLocation);
    } else if (isStmtType(type)) {
      return new PlaceholderStatement(path, type, sourceLocation);
    } else if (isExprType(type)) {
      return new PlaceholderExpr(path, type, sourceLocation);
    } else {
      return new PlaceholderNode(path, type, sourceLocation);
    }
  }

  static SyntaxType paramSyntaxType(Parser parser, String name) {
    for (List<MacroParam> params : parser.macroContext) {
      for (MacroParam param : params) {
        if (param.name().name.equals(name)) {
          return param.type();
        }
      }
    }
    return BasicSyntaxType.INVALID;
  }

  static Node createMacroMatch(SyntaxType resultType, List<MacroMatch.Choice> choices,
                               Node defaultChoice, SourceLocation sourceLocation) {
    var macroMatch = new MacroMatch(resultType, choices, defaultChoice, sourceLocation);
    if (isDefType(resultType)) {
      return new MacroMatchDefinition(macroMatch);
    } else if (isStmtType(resultType)) {
      return new MacroMatchStatement(macroMatch);
    } else {
      return new MacroMatchExpr(macroMatch);
    }
  }

  /**
   * Returns either the given macro's parameter types or, if null, the given syntax type's
   * {@link ProjectionType#arguments}.
   */
  static Iterator<SyntaxType> instanceParamTypes(MacroOrPlaceholder macroOrPlaceholder) {
    if (macroOrPlaceholder instanceof Macro macro) {
      return new Iterator<>() {
        final Iterator<MacroParam> params = macro.params().iterator();

        @Override
        public boolean hasNext() {
          return params.hasNext();
        }

        @Override
        public SyntaxType next() {
          return params.next().type();
        }
      };
    }
    return ((MacroPlaceholder) macroOrPlaceholder).syntaxType().arguments.iterator();
  }

  /**
   * Checks whether the token is an identifier token.
   * Since some keywords are allowed as identifiers, this is not as simple as checking the type.
   * Must be kept in sync with the "allowedIdentifierKeywords" rule.
   *
   * @param token The token to inspect
   * @return Whether the token is suitable for "identifier" substitution
   */
  static boolean isIdentifierToken(Token token) {
    return token.kind == Parser._identifierToken
        || token.kind == Parser._T_BOOL
        || token.kind == Parser._REGISTER
        || token.kind == Parser._EXCEPTION;
  }

  /**
   * Pre-parses the next few tokens to determine the type of the following placeholder / macro.
   * Before: parser must be in a state where the lookahead token is the "$" symbol.
   * After: parser is in the same state as before.
   */
  static boolean isMacroReplacementOfType(Parser parser, BasicSyntaxType syntaxType) {
    if (parser.la.kind == Parser._EXTEND_ID) {
      return BasicSyntaxType.ID.isSubTypeOf(syntaxType);
    }
    if (parser.la.kind == Parser._ID_TO_STR) {
      return BasicSyntaxType.STR.isSubTypeOf(syntaxType);
    }
    SyntaxType macroMatchType = macroMatchType(parser);
    if (macroMatchType != null) {
      return macroMatchType.isSubTypeOf(syntaxType);
    }
    if (parser.la.kind != Parser._SYM_DOLLAR) {
      return false;
    }
    parser.scanner.ResetPeek();
    var token = parser.scanner.Peek();
    var nextToken = parser.scanner.Peek();
    var foundMacro = parser.symbolTable.getMacro(token.val);
    if (foundMacro != null) {
      parser.scanner.ResetPeek();
      if (nextToken.kind == Parser._SYM_PAREN_OPEN) {
        return foundMacro.returnType().isSubTypeOf(syntaxType);
      } else {
        return false;
      }
    }
    SyntaxType paramType = paramSyntaxType(parser, token.val);
    while (true) {
      if (paramType instanceof BasicSyntaxType basicSyntaxType) {
        parser.scanner.ResetPeek();
        return basicSyntaxType.isSubTypeOf(syntaxType);
      } else if (paramType instanceof RecordType recordType && nextToken.kind == Parser._SYM_DOT) {
        token = parser.scanner.Peek();
        nextToken = parser.scanner.Peek();
        paramType = recordType.findEntry(token.val);
      } else if (paramType instanceof ProjectionType projectionType
          && nextToken.kind == Parser._SYM_PAREN_OPEN) {
        parser.scanner.ResetPeek();
        return projectionType.resultType.isSubTypeOf(syntaxType);
      } else {
        parser.scanner.ResetPeek();
        return false;
      }
    }
  }

  private static @Nullable SyntaxType macroMatchType(Parser parser) {
    parser.scanner.ResetPeek();
    boolean isMacroMatch = parser.la.kind == Parser._MATCH
        && parser.scanner.Peek().kind == Parser._SYM_COLON;
    if (isMacroMatch) {
      String type = parser.scanner.Peek().val;
      for (var basicType : BasicSyntaxType.values()) {
        if (basicType.name().equals(type)) {
          parser.scanner.ResetPeek();
          return basicType;
        }
      }
    }
    parser.scanner.ResetPeek();
    return null;
  }

  static boolean assertSyntaxType(Parser parser, @Nullable Node node, SyntaxType requiredType,
                                  String message) {
    if (node != null && !node.syntaxType().isSubTypeOf(requiredType)) {
      parser.errors.SemErr(parser.t.line, parser.t.col,
          message + ": Required %s, node is %s".formatted(requiredType, node.syntaxType()));
      return false;
    }
    return true;
  }
}
