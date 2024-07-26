package vadl.ast;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.utils.SourceLocation;

class ParserUtils {

  private static final Ungrouper UNGROUPER = new Ungrouper();
  static boolean[] BIN_OPS;
  static boolean[] BIN_OPS_EXCEPT_GT;
  static boolean[] BIN_OPS_EXCEPT_IN;

  static {
    BIN_OPS = new boolean[Parser.maxT];
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
   * it will reorder the expression's operand according to {@link BinaryExpr#reorder(BinaryExpr)}
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
   * Will ungroup expressions if the parser is not currently parsing a model.
   *
   * @see Ungrouper#ungroup(Expr)
   */
  static Expr ungroup(Parser parser, Expr expr) {
    if (parser.insideMacro) {
      return expr;
    }
    return UNGROUPER.ungroup(expr);
  }

  /**
   * Converts the parser's current token position to a vadl location.
   */
  static SourceLocation locationFromToken(Parser parser) {
    return new SourceLocation(
        parser.sourceFile,
        new SourceLocation.Position(parser.t.line, parser.t.col),
        new SourceLocation.Position(parser.t.line, parser.t.col + parser.t.val.length()));
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

    var macro = parser.symbolTable.getMacro(identifier.name);
    if (macro == null) {
      parser.errors.SemErr(parser.t.line, parser.t.col,
          "No macro named `%s` exists.".formatted(identifier.name));
      return unexpanded;
    }

    // The macro itself was invalid but an error for it was already issued so we silently abort the expansion here.
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
              "The macro's `%s` parameter `%s` expects a `%s` but the argument provided is of type `%s`.".formatted(
                  identifier.name, param.name().name, param.type(), argType));
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
    var body = macro.body();
    if (body instanceof Expr expr) {
      var expander = new MacroExpander(argMap, parser.symbolTable);
      body = expander.expandExpr(expr);
      body = new GroupExpr((Expr) body);
    } else if (body instanceof Definition def) {
      var expander = new MacroExpander(argMap, parser.symbolTable);
      body = expander.expandDefinition(def);
    } else {
      throw new RuntimeException("Expanding non expressions are not yet implemented");
    }
    return body;
  }

  static void pushScope(Parser parser) {
    parser.symbolTable = parser.symbolTable.createChild();
  }

  static void pushFormatScope(Parser parser, Node formatId) {
    if (formatId instanceof Identifier id) {
      parser.symbolTable = parser.symbolTable.createFormatScope(id);
    } else {
      pushScope(parser);
    }
  }

  static void pushInstructionScope(Parser parser, Node instrId) {
    if (instrId instanceof Identifier id) {
      parser.symbolTable = parser.symbolTable.createInstructionScope(id);
    } else {
      pushScope(parser);
    }
  }

  static void popScope(Parser parser) {
    parser.symbolTable = Objects.requireNonNull(parser.symbolTable.parent);
  }
}
