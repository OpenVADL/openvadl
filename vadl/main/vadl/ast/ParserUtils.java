package vadl.ast;

import java.util.List;
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
    return type.isSubTypeOf(BasicSyntaxType.Ex());
  }

  static boolean isDefType(SyntaxType type) {
    return type.isSubTypeOf(BasicSyntaxType.IsaDefs());
  }

  static boolean isStmtType(SyntaxType type) {
    return type.isSubTypeOf(BasicSyntaxType.Stats());
  }

  static Node createMacroInstance(Macro macro, List<Node> args, SourceLocation sourceLocation) {
    if (isDefType(macro.returnType())) {
      return new MacroInstanceDefinition(macro, args, sourceLocation);
    } else if (isStmtType(macro.returnType())) {
      return new MacroInstanceStatement(macro, args, sourceLocation);
    } else {
      return new MacroInstanceExpr(macro, args, sourceLocation);
    }
  }

  static Node createPlaceholder(SyntaxType type, IsId path, SourceLocation sourceLocation) {
    if (isDefType(type)) {
      return new PlaceholderDefinition(path, sourceLocation);
    } else if (isStmtType(type)) {
      return new PlaceholderStatement(path, sourceLocation);
    } else {
      return new PlaceholderExpr(path, sourceLocation);
    }
  }

  static Node narrowNode(Node node) {
    if (node instanceof StatementList statementList) {
      return statementList.items.get(0);
    }
    return node;
  }

  /**
   * Pre-parses the next few tokens to determine the type of the following placeholder / macro.
   * Before: parser must be in a state where the lookahead token is the "$" symbol.
   * After: parser is in the same state as before.
   */
  static boolean isMacroParamType(Parser parser, SyntaxType syntaxType) {
    if (parser.la.kind != Parser._SYM_DOLLAR) {
      return false;
    }
    var parserSnapshot = ParserSnapshot.create(parser);
    var scannerSnapshot = ScannerSnapshot.create(parser.scanner);
    var maxExpr = parser.maximumMacroExpr();
    parserSnapshot.reset(parser);
    scannerSnapshot.reset(parser.scanner);

    if (maxExpr instanceof MacroInstanceExpr macroInstanceExpr) {
      var macro = parser.symbolTable.getMacro(macroInstanceExpr.macro.name().name);
      return macro != null && macro.returnType().isSubTypeOf(syntaxType);
    }
    if (maxExpr instanceof PlaceholderExpr placeholderExpr) {
      for (List<MacroParam> params : parser.macroContext) {
        for (MacroParam param : params) {
          if (param.name().name.equals(placeholderExpr.placeholder.path().pathToString())) {
            return param.type().isSubTypeOf(syntaxType);
          }
        }
      }
    }
    return maxExpr.syntaxType().isSubTypeOf(syntaxType);
  }

  record ParserSnapshot(Token t, Token la) {
    static ParserSnapshot create(Parser parser) {
      return new ParserSnapshot(parser.t, parser.la);
    }

    void reset(Parser parser) {
      parser.t = t;
      parser.la = la;
    }
  }

  record ScannerSnapshot(Token t, int ch, int col, int line, int charPos, int bufferPos) {
    static ScannerSnapshot create(Scanner scanner) {
      return new ScannerSnapshot(scanner.t, scanner.ch, scanner.col, scanner.line, scanner.charPos,
          scanner.buffer.getPos());
    }

    void reset(Scanner scanner) {
      scanner.t = t;
      scanner.ch = ch;
      scanner.col = col;
      scanner.line = line;
      scanner.buffer.setPos(bufferPos);
    }
  }
}
