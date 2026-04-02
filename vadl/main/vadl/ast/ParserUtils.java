// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.ast;

import static java.util.Objects.requireNonNull;
import static vadl.error.Diagnostic.error;

import java.math.BigInteger;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.error.Diagnostic;
import vadl.error.DiagnosticList;
import vadl.utils.Levenshtein;
import vadl.utils.SourceLocation;
import vadl.utils.WithLocation;

class ParserUtils {

  static String VADL_EXTENSION = ".vadl";

  static boolean[] NO_OPS;
  static boolean[] BIN_OPS;
  static boolean[] BIN_OPS_EXCEPT_GT;
  static boolean[] BIN_OPS_EXCEPT_IN;
  static boolean[] UN_OPS;
  // an array of literal and symbol representations that are used for a
  // good parse error message such as "`(` expected".
  static String[] EXPECTED_STRS;

  // Must be kept in sync with allowedIdentifierKeywords
  static boolean[] ID_TOKENS;

  // Dummy nodes - for use in scenarios where the parser encountered an error, but since the parser
  // keeps going, using null would lead to a NullPointerException.
  static Identifier DUMMY_ID = new Identifier("dummy", SourceLocation.INVALID_SOURCE_LOCATION);
  static Expr DUMMY_EXPR = DUMMY_ID;
  static Definition DUMMY_DEF =
      new ConstantDefinition(DUMMY_ID, null, DUMMY_EXPR, SourceLocation.INVALID_SOURCE_LOCATION);
  static Statement DUMMY_STAT = new CallStatement(DUMMY_ID);

  static {
    NO_OPS = new boolean[Parser.maxT + 1];

    BIN_OPS = NO_OPS.clone();
    BIN_OPS[Parser._SYM_BINAND] = true;
    BIN_OPS[Parser._SYM_BINOR] = true;
    BIN_OPS[Parser._SYM_CARET] = true;
    BIN_OPS[Parser._SYM_DIV] = true;
    BIN_OPS[Parser._SYM_ELEM_OF] = true;
    BIN_OPS[Parser._SYM_EQ] = true;
    BIN_OPS[Parser._SYM_GT] = true;
    BIN_OPS[Parser._SYM_GTE] = true;
    BIN_OPS[Parser._SYM_IN] = true;
    BIN_OPS[Parser._SYM_LOGAND] = true;
    BIN_OPS[Parser._SYM_LOGOR] = true;
    BIN_OPS[Parser._SYM_LONG_MUL] = true;
    BIN_OPS[Parser._SYM_LT] = true;
    BIN_OPS[Parser._SYM_LTE] = true;
    BIN_OPS[Parser._SYM_MINUS] = true;
    BIN_OPS[Parser._SYM_MOD] = true;
    BIN_OPS[Parser._SYM_MUL] = true;
    BIN_OPS[Parser._SYM_NEQ] = true;
    BIN_OPS[Parser._SYM_NIN] = true;
    BIN_OPS[Parser._SYM_NOT_ELEM_OF] = true;
    BIN_OPS[Parser._SYM_PLUS] = true;
    BIN_OPS[Parser._SYM_ROTL] = true;
    BIN_OPS[Parser._SYM_ROTR] = true;
    BIN_OPS[Parser._SYM_SAT_ADD] = true;
    BIN_OPS[Parser._SYM_SAT_SUB] = true;
    BIN_OPS[Parser._SYM_SHL] = true;
    BIN_OPS[Parser._SYM_SHR] = true;

    BIN_OPS_EXCEPT_GT = BIN_OPS.clone();
    BIN_OPS_EXCEPT_GT[Parser._SYM_GT] = false;

    BIN_OPS_EXCEPT_IN = BIN_OPS.clone();
    BIN_OPS_EXCEPT_IN[Parser._SYM_IN] = false;

    UN_OPS = NO_OPS.clone();
    UN_OPS[Parser._SYM_MINUS] = true;
    UN_OPS[Parser._SYM_EXCL] = true;
    UN_OPS[Parser._SYM_TILDE] = true;

    ID_TOKENS = NO_OPS.clone();
    ID_TOKENS[Parser._identifierToken] = true;
    ID_TOKENS[Parser._ADDRESS] = true;
    ID_TOKENS[Parser._ALIAS] = true;
    ID_TOKENS[Parser._ALIGN] = true;
    ID_TOKENS[Parser._APPEND] = true;
    ID_TOKENS[Parser._BINARY] = true;
    ID_TOKENS[Parser._CALL] = true;
    ID_TOKENS[Parser._CONSTANT] = true;
    ID_TOKENS[Parser._EXCEPTION] = true;
    ID_TOKENS[Parser._FETCH] = true;
    ID_TOKENS[Parser._GLOBAL] = true;
    ID_TOKENS[Parser._GROUP] = true;
    ID_TOKENS[Parser._INSTRUCTION] = true;
    ID_TOKENS[Parser._INT] = true;
    ID_TOKENS[Parser._LONG] = true;
    ID_TOKENS[Parser._MEMORY] = true;
    ID_TOKENS[Parser._MAX] = true;
    ID_TOKENS[Parser._NONE] = true;
    ID_TOKENS[Parser._NOP] = true;
    ID_TOKENS[Parser._OPERATION] = true;
    ID_TOKENS[Parser._PREDICTION] = true;
    ID_TOKENS[Parser._READ] = true;
    ID_TOKENS[Parser._REGISTER] = true;
    ID_TOKENS[Parser._RETURN] = true;
    ID_TOKENS[Parser._SEQUENCE] = true;
    ID_TOKENS[Parser._SIGNED] = true;
    ID_TOKENS[Parser._SIZE_T] = true;
    ID_TOKENS[Parser._SOURCE] = true;
    ID_TOKENS[Parser._STAGE] = true;
    ID_TOKENS[Parser._STARTUP] = true;
    ID_TOKENS[Parser._RESET] = true;
    ID_TOKENS[Parser._STOP] = true;
    ID_TOKENS[Parser._SYM_IN] = true;
    ID_TOKENS[Parser._TYPE] = true;
    ID_TOKENS[Parser._T_BIN] = true;
    ID_TOKENS[Parser._T_BIN_OP] = true;
    ID_TOKENS[Parser._T_BOOL] = true;
    ID_TOKENS[Parser._T_CALL_EX] = true;
    ID_TOKENS[Parser._T_COMMON_DEFS] = true;
    ID_TOKENS[Parser._T_ENCS] = true;
    ID_TOKENS[Parser._T_ID] = true;
    ID_TOKENS[Parser._T_INT] = true;
    ID_TOKENS[Parser._T_ISA_DEFS] = true;
    ID_TOKENS[Parser._T_LIT] = true;
    ID_TOKENS[Parser._T_STAT] = true;
    ID_TOKENS[Parser._T_STATS] = true;
    ID_TOKENS[Parser._T_STR] = true;
    ID_TOKENS[Parser._T_SYM_EX] = true;
    ID_TOKENS[Parser._T_UN_OP] = true;
    ID_TOKENS[Parser._T_VAL] = true;
    ID_TOKENS[Parser._TRANSLATION] = true;
    ID_TOKENS[Parser._UNSIGNED] = true;
    ID_TOKENS[Parser._WIDTH] = true;
    ID_TOKENS[Parser._WRITE] = true;


    EXPECTED_STRS = new String[NO_OPS.length];
    EXPECTED_STRS[Parser._SYM_ARROW] = "->";
    EXPECTED_STRS[Parser._SYM_ASSIGN] = ":=";
    EXPECTED_STRS[Parser._SYM_AT] = "@";
    EXPECTED_STRS[Parser._SYM_BIGARROW] = "=>";
    EXPECTED_STRS[Parser._SYM_BINAND] = "&";
    EXPECTED_STRS[Parser._SYM_BINOR] = "|";
    EXPECTED_STRS[Parser._SYM_BRACE_CLOSE] = "}";
    EXPECTED_STRS[Parser._SYM_BRACE_OPEN] = "{";
    EXPECTED_STRS[Parser._SYM_BRACK_CLOSE] = "]";
    EXPECTED_STRS[Parser._SYM_BRACK_OPEN] = "[";
    EXPECTED_STRS[Parser._SYM_CARET] = "^";
    EXPECTED_STRS[Parser._SYM_COLON] = ":";
    EXPECTED_STRS[Parser._SYM_COMMA] = ",";
    EXPECTED_STRS[Parser._SYM_DIV] = "/";
    EXPECTED_STRS[Parser._SYM_DOLLAR] = "$";
    EXPECTED_STRS[Parser._SYM_DOT] = ".";
    EXPECTED_STRS[Parser._SYM_ELEM_OF] = "∈";
    EXPECTED_STRS[Parser._SYM_EQ] = "=";
    EXPECTED_STRS[Parser._SYM_EXCL] = "!";
    EXPECTED_STRS[Parser._SYM_GT] = ">";
    EXPECTED_STRS[Parser._SYM_GTE] = ">=";
    EXPECTED_STRS[Parser._SYM_IN] = "in";
    EXPECTED_STRS[Parser._SYM_LOGAND] = "&&";
    EXPECTED_STRS[Parser._SYM_LOGOR] = "||";
    EXPECTED_STRS[Parser._SYM_LONG_MUL] = "*#";
    EXPECTED_STRS[Parser._SYM_LT] = "<";
    EXPECTED_STRS[Parser._SYM_LTE] = "<=";
    EXPECTED_STRS[Parser._SYM_MINUS] = "-";
    EXPECTED_STRS[Parser._SYM_MOD] = "%";
    EXPECTED_STRS[Parser._SYM_MUL] = "*";
    EXPECTED_STRS[Parser._SYM_NAMESPACE] = "::";
    EXPECTED_STRS[Parser._SYM_NEQ] = "!=";
    EXPECTED_STRS[Parser._SYM_NIN] = "!in";
    EXPECTED_STRS[Parser._SYM_NOT_ELEM_OF] = "∉";
    EXPECTED_STRS[Parser._SYM_PAREN_CLOSE] = ")";
    EXPECTED_STRS[Parser._SYM_PAREN_OPEN] = "(";
    EXPECTED_STRS[Parser._SYM_PLUS] = "+";
    EXPECTED_STRS[Parser._SYM_PLUS_EQ] = "+=";
    EXPECTED_STRS[Parser._SYM_QUESTION] = "?";
    EXPECTED_STRS[Parser._SYM_RANGE] = "..";
    EXPECTED_STRS[Parser._SYM_ROTL] = "<<>";
    EXPECTED_STRS[Parser._SYM_ROTR] = "<>>";
    EXPECTED_STRS[Parser._SYM_SAT_ADD] = "+|";
    EXPECTED_STRS[Parser._SYM_SAT_SUB] = "-|";
    EXPECTED_STRS[Parser._SYM_SEMICOLON] = ";";
    EXPECTED_STRS[Parser._SYM_SHL] = "<<";
    EXPECTED_STRS[Parser._SYM_SHR] = ">>";
    EXPECTED_STRS[Parser._SYM_TILDE] = "~";
    EXPECTED_STRS[Parser._SYM_UNDERSCORE] = "_";
    // set literals produced by scanner
    for (var l : Scanner.literals.entrySet()) {
      EXPECTED_STRS[l.getValue()] = l.getKey();
    }
  }

  /**
   * Given a cast expression, unpacks the cast value and, if it is a binary expression,
   * reorders the tree as to only apply the cast to the right side of the binary expression.
   * This represents the cast operator having the strongest precedence of all binary operators.
   *
   * @param expr A {@link CastExpr}, optionally containing a {@link BinaryExpr}.
   * @return A cast expression with the proper precedence applied.
   */
  static Expr reorderCastExpr(Expr expr) {
    if (expr instanceof CastExpr castExpr) {
      if (castExpr.value instanceof BinaryExpr binExpr) {
        return new BinaryExpr(binExpr.left, binExpr.operator,
            new CastExpr(binExpr.right, castExpr.typeLiteral));
      }
    }
    return expr;
  }

  static Expr reorderBinaryExpr(Expr expr) {
    if (expr instanceof BinaryExpr binExpr) {
      var canReorder = canReorder(binExpr);
      if (canReorder) {
        return BinaryExpr.reorder(binExpr);
      }
    }
    return expr;
  }

  /**
   * A binary expression can only be reordered if none of the operators in the binary expression
   * tree use a macro or a placeholder as the operator.
   *
   * @param binExpr The top of a binary expression tree
   * @return Whether the passed binary expression tree can be reordered
   */
  static boolean canReorder(BinaryExpr binExpr) {
    if (!(binExpr.operator instanceof BinOp)) {
      return false;
    }
    if (binExpr.left instanceof BinaryExpr leftBin && !canReorder(leftBin)) {
      return false;
    }
    if (binExpr.right instanceof BinaryExpr rightBin && !canReorder(rightBin)) {
      return false;
    }
    return true;
  }

  /**
   * Converts the parser's current token position to a vadl location.
   */
  static SourceLocation locationFromToken(Parser parser, Token token) {
    return new SourceLocation(
        parser.sourceFile,
        new SourceLocation.Position(token.line, token.col),
        new SourceLocation.Position(token.line, token.col + token.val.length() - 1));
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
    } else if (isExprType(macroOrPlaceholder.returnType())) {
      return new MacroInstanceExpr(macroOrPlaceholder, args, sourceLocation);
    } else {
      return new MacroInstanceNode(macroOrPlaceholder, args, sourceLocation);
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
    } else if (isExprType(resultType)) {
      return new MacroMatchExpr(macroMatch);
    } else {
      return new MacroMatchNode(macroMatch);
    }
  }

  static Node createMacroReference(Parser parser, Identifier id) {
    @Nullable Macro macro = parser.macroTable.getMacro(id.name);
    if (macro == null) {
      parser.diagnostics.add(
          Diagnostic.error("Unknown Model `%s`".formatted(id.name), id)
              .help("Make sure the macro is defined before (above) they are used.")
              .build());
      return DUMMY_ID;
    } else {
      List<SyntaxType> params = new ArrayList<>(macro.params().size());
      for (MacroParam param : macro.params()) {
        params.add(param.type());
      }
      return new MacroReference(macro, new ProjectionType(params, macro.returnType()),
          id.location());
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
   *
   * @param token The token to inspect
   * @return Whether the token is a suitable "identifier"
   * @see ParserUtils#ID_TOKENS
   */
  static boolean isIdentifierToken(Token token) {
    return ID_TOKENS[token.kind];
  }

  private static final Pattern identifierPattern = Pattern.compile("[a-zA-Z][a-zA-Z0-9_]*");

  /**
   * Checks whether a string can be used as an indentifier.
   *
   * @param text to be checked.
   * @return whether it would be a valid identifier.
   */
  static boolean isValidIdentifier(String text) {

    if (!identifierPattern.matcher(text).matches()) {
      return false;
    }

    // Only some keywords are allowed as tokens.
    var tokenId = Scanner.literals.get(text);
    if (tokenId != null && !ID_TOKENS[tokenId]) {
      return false;
    }

    return true;
  }

  /**
   * Pre-parses the next few tokens to determine the type of the following placeholder / macro.
   * Before: parser must be in a state where the lookahead token is the "$" symbol.
   * After: parser is in the same state as before.
   */
  static boolean isMacroReplacementOfType(Parser parser, BasicSyntaxType syntaxType) {
    if (parser.la.kind == Parser._AS_ID) {
      return BasicSyntaxType.ID.isSubTypeOf(syntaxType);
    }
    if (parser.la.kind == Parser._AS_STR) {
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
    var foundMacro = parser.macroTable.getMacro(token.val);
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
        if (basicType.getName().equals(type)) {
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
    if (requiredType == BasicSyntaxType.INVALID) {
      // This only happens because of a followup error that already got reported, no need to report
      // another diagnostic.
      return false;
    }

    if (node != null && !node.syntaxType().isSubTypeOf(requiredType)) {
      parser.diagnostics.add(
          Diagnostic.error("SyntaxType Mismatch", node)
              .description("%s: Required `%s`, but got `%s`", message, requiredType,
                  node.syntaxType())
              .build());
      return false;
    }
    return true;
  }

  static Diagnostic unknownSyntaxTypeError(String name, SymbolTable macroTable,
                                           SourceLocation location) {
    // Initially add the basic types and custom defined in scope.
    var available = Arrays.stream(BasicSyntaxType.values())
        .map(BasicSyntaxType::getName)
        .filter(n -> !n.contains("Invalid"))
        .collect(Collectors.toSet());
    available.addAll(
        macroTable.allMacroSymbolNamesOf(RecordTypeDefinition.class, ModelTypeDefinition.class));

    return error("Unknown syntax type: `%s`".formatted(name), location)
        .locationDescription(location, "No syntax type with this name exists.")
        .suggestions(Levenshtein.suggestions(name, available))
        .build();
  }

  static Diagnostic tooManyMacroArgumentsError(@Nullable Macro macro, SyntaxType type,
                                               SourceLocation location) {
    // Unfortunately, we need the types of the macro parameters to parse the invocation to
    // completion. But if more arguments are provided than parameter are defined we cannot parse
    // them and therefore only know that too many exist but not how many were provided.
    // The macro may not exist if we are calling a macro that is passed to the current macro, in
    // which case we need to rely on the type.
    var builder = error("Invalid Model Invocation", location);
    if (macro != null) {
      builder.locationDescription(location,
          "Model `%s` only expected %d arguments but, you provided at least %d.",
          macro.name().name,
          macro.params().size(), macro.params().size() + 1);
    } else {
      var argCount = switch (type) {
        case ProjectionType pt -> pt.arguments.size();
        default -> 1;
      };

      builder.locationDescription(location,
          "The model only expected %d arguments but, you provided at least %d.",
          argCount, argCount + 1);
    }
    return builder.build();
  }

  private static boolean isPlaceholder(Node n) {
    return n instanceof PlaceholderNode
        || n instanceof PlaceholderDefinition
        || n instanceof PlaceholderExpr
        || n instanceof PlaceholderStatement;
  }

  private static <T> T castOrDummy(Parser parser, Node node,
                                   Class<T> type,
                                   T dummy,
                                   String expected) {
    if (type.isInstance(node)) {
      return type.cast(node);
    }

    String message;
    if (isPlaceholder(node)) {
      var sb = new StringBuilder("");
      node.prettyPrint(0, sb);
      var name = sb.toString();

      parser.diagnostics.add(
          Diagnostic.error("Unknown Model `%s`".formatted(name), node)
              .help("Make sure the macro is defined before (above) they are used.")
              .build());

    } else {
      message =
          "Expected node of type " + expected + ", received "
              + node.syntaxType().print() + " - " + node;
      parser.diagnostics.add(
          Diagnostic.error("SyntaxType Mismatch", node)
              .description("%s", message)
              .build());
    }

    return dummy;
  }

  static Expr castExpr(Parser p, Node n) {
    return castOrDummy(p, n, Expr.class, DUMMY_EXPR, "Expr");
  }

  static StringLiteral castForceStringLiteral(Parser p, Node n) {
    return castOrDummy(p, n, StringLiteral.class, new StringLiteral(""), "Str");
  }

  static IsEncs castEncs(Parser p, Node n) {
    return castOrDummy(p, n, IsEncs.class,
        new EncodingDefinition.EncodingField(DUMMY_ID, DUMMY_ID), "Encs");
  }

  static IdentifierOrPlaceholder castId(Parser p, Node n) {
    return castOrDummy(p, n, IdentifierOrPlaceholder.class, DUMMY_ID, "Id");
  }

  static IsBinOp castBinOp(Parser p, Node n) {
    return castOrDummy(p, n, IsBinOp.class, new BinOp(Operator.Xor, n.location()), "BinOp");
  }

  static Definition castCommonDef(Parser p, Node n) {
    return (n instanceof Definition d && d.syntaxType().isSubTypeOf(BasicSyntaxType.COMMON_DEFS))
        ? d : castOrDummy(p, n, Definition.class, DUMMY_DEF, "CommonDefs");
  }

  static Definition castIsaDef(Parser p, Node n) {
    return (n instanceof Definition d && d.syntaxType().isSubTypeOf(BasicSyntaxType.ISA_DEFS))
        ? d : castOrDummy(p, n, Definition.class, DUMMY_DEF, "IsaDefs");
  }

  static Statement castStat(Parser p, Node n) {
    return castOrDummy(p, n, Statement.class, DUMMY_STAT, "Stat");
  }

  static Statement castStats(Parser p, Node n) {
    return castOrDummy(p, n, Statement.class, DUMMY_STAT, "Stats");
  }

  static Node expandNode(Parser parser, Node node) {
    var macroExpander = new MacroExpander(Map.of(), parser.macroOverrides, node.location());
    var expanded = macroExpander.expandNode(node, parser.ast);
    if (parser.macroContext.isEmpty()) {
      // TODO This is necessary to completely copy all nodes to not cause issues
      //  in symbol collection - find out why
      return macroExpander.expandNode(expanded, parser.ast);
    }
    return expanded;
  }

  /**
   * Assembly definitions can be written with multiple identifiers to be bound to multiple
   * (pseudo) instructions. However, for correct further processing they need to be expanded into
   * multiple definitions.
   *
   * @param isaDefs to be expanded.
   * @return returns the original isaDefs with the Assemblies expanded.
   */
  static List<Definition> expandAssemblyDefinitionsInIsa(List<Definition> isaDefs) {
    return isaDefs.stream()
        .flatMap(def -> {
          if (def instanceof AssemblyDefinition assembly) {
            return new MacroExpander(Map.of(), Map.of(), def.location())
                .expandAssemblies(assembly).stream();
          }
          return Stream.of(def);
        })
        .collect(Collectors.toCollection(ArrayList::new));
  }

  /**
   * Defines all provided macro definitions in the provided macroTable.
   *
   * @param macroTable  to be modified.
   * @param definitions to be inserted.
   */
  static void readMacroSymbols(SymbolTable macroTable, List<Definition> definitions) {
    for (Definition definition : definitions) {
      if (definition instanceof DefinitionList list) {
        readMacroSymbols(macroTable, list.items);
      } else if (definition instanceof ModelDefinition modelDefinition) {
        macroTable.defineSymbol(modelDefinition);
      } else if (definition instanceof ModelTypeDefinition modelTypeDefinition) {
        macroTable.defineSymbol(modelTypeDefinition);
      } else if (definition instanceof RecordTypeDefinition recordTypeDefinition) {
        macroTable.defineSymbol(recordTypeDefinition);
      }
    }
  }

  /**
   * Recursively reads all macros in each of the extending instruction definitions.
   *
   * @param macroTable the macro table that should be fed with found macro symbols
   * @param isa        the isa that should be (recursively) traversed to find all macro definitions.
   */
  static void readMacroSymbols(SymbolTable macroTable, InstructionSetDefinition isa) {
    readMacroSymbols(macroTable, isa.definitions);
    // FIXME: This is not optimal as we traverse an ISA potentially multiple times.
    // as we don't have access to the macroTable of the referenced ISA, we must
    // do the traversal again.
    for (IsId extending : isa.extending) {
      var extendingIsa = (InstructionSetDefinition) requireNonNull(extending.target());
      readMacroSymbols(macroTable, extendingIsa);
    }
  }

  static void verifyCorrectModelOverride(Parser parser, ModelDefinition def,
                                         SourceLocation location) {
    if (parser.macroOverrides.containsKey(def.id.pathToString()) && !def.returnType.equals(
        BasicSyntaxType.ID)) {
      parser.diagnostics.add(
          error("Invalid Model Override", location)
              .locationNote(location,
                  "This model was overridden but only models returning `Id` can be overridden.")
              .build()
      );
    }
  }

  /**
   * Loads the referenced module and makes any given symbols available in the current module.
   * Either a {@code fileId} or a {@code filePath} MUST be specified.
   * If a {@code fileId} is used, the id will be resolved as a sibling {@code {id}.vadl} file.
   * If a {@code filePath} is used, the file path will be resolved relative to the specification.
   *
   * @param parser          The instance of the parser parsing the current module
   * @param fileId          An optional name of the referenced .vadl file
   * @param filePath        An optional path to the referenced specification file
   * @param importedSymbols The list of symbol paths to import
   * @param args            A list of arguments to pass to the imported module
   *                        for model substitution
   * @return An import definition node
   */
  static Definition importModules(Parser parser,
                                  @Nullable Identifier fileId, @Nullable StringLiteral filePath,
                                  List<List<Identifier>> importedSymbols,
                                  List<StringLiteral> args, SourceLocation loc) {
    var modulePath = filePath == null
        ? resolveImportPath(parser, requireNonNull(fileId))
        : resolveImportPath(parser, filePath.value, filePath);
    if (modulePath == null) {
      return DUMMY_DEF;
    }

    if (importedSymbols.isEmpty() || importedSymbols.stream().allMatch(List::isEmpty)) {
      WithLocation fileLocation = requireNonNull(filePath == null ? fileId : filePath);
      throw error("Invalid Import", fileLocation)
          .locationDescription(fileLocation,
              "The import only describes the file to import from but not what to import.")
          .locationNote(fileLocation,
              "VADL doesn't have a wildcard import, you always have to define what to import")
          .build();
    }

    var macroOverrides = new HashMap<String, String>();
    for (StringLiteral arg : args) {
      var keyValue = arg.value.split("=", 2);
      if (keyValue.length != 2) {
        throw error("Invalid Import", arg)
            .locationDescription(arg,
                "Macro overrides must have the form `<macro-name>=<substitute>`.")
            .build();
      }
      macroOverrides.put(keyValue[0], keyValue[1]);
    }
    try {
      parser.ast.timingRecorder.pauseRecording();
      var ast = VadlParser.parse(modulePath, parser.fileSystem, macroOverrides);
      parser.macroTable.importFrom(ast, importedSymbols);
      parser.ast.timingRecorder.importAllTimings(ast.timingRecorder);
      parser.ast.timingRecorder.continueRecording();
      return new ImportDefinition(ast, importedSymbols, fileId, filePath, args, loc);
    } catch (DiagnosticList | Diagnostic e) {
      throw e;
    }
  }

  static @Nullable Path resolveImportPath(Parser parser, Identifier importPath) {
    return resolveImportPath(parser, importPath.name, importPath);
  }

  /**
   * Resolves a usable path of the file to import.
   * Since files are always imported relative to the caller this takes into account the path of the
   * currently open file.
   *
   * @param parser      of the current file.
   * @param name        to resolve, might be a path.
   * @param location    of the import statement, used for errors.
   * @return            a path to import.
   */
  static @Nullable Path resolveImportPath(Parser parser, String name, WithLocation location) {
    var resolutionPath = Objects.requireNonNullElse(parser.resolutionPath, parser.sourceFile);
    var relativeToSpec = resolutionPath.resolveSibling(name);
    if (parser.fileSystem.exists(relativeToSpec)) {
      return relativeToSpec;
    }
    var withAppendedExtension = resolutionPath.resolveSibling(name + VADL_EXTENSION);
    if (parser.fileSystem.exists(withAppendedExtension)) {
      return withAppendedExtension;
    }
    parser.diagnostics.add(Diagnostic.error("Import Error", location)
        .description("Could not resolve file path: \"%s\"", name)
        .build());
    return null;
  }

  /**
   * Return the basename of a path.
   * Example: /home/flo/abc.txt -> abc
   *
   * @param path to extract the files basename
   * @return the basename
   */
  static String baseName(Path path) {
    var name = path.getFileName().toString();
    return name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name;
  }

  /**
   * Checks whether the given token is a valid (concrete) unary operator token.
   * Needs to be kept in sync with the {@code unaryOperator} rule.
   *
   * @param token The token to check
   * @return Whether token is a unary operator token
   */
  static boolean isUnaryOperator(Token token) {
    return UN_OPS[token.kind];
  }

  /**
   * Builds the list of imported symbol paths for an import declaration.
   * For example, {@code "./file.vadl"::ISA_A::{Enum_A, Enum_B}} will lead to the result
   * {@code [["ISA_A", "Enum_A"], ["ISA_A", "Enum_B"]]}
   *
   * @param segments   The segments of the import declaration, missing any leading file segment and
   *                   trailing symbol lists
   * @param symbolList The trailing list of imported symbols
   * @return A list of symbol paths to import
   */
  static List<List<Identifier>> importedSymbols(List<Identifier> segments,
                                                List<List<Identifier>> symbolList) {
    if (symbolList.isEmpty()) {
      return List.of(segments);
    } else {
      var result = new ArrayList<List<Identifier>>();
      for (List<Identifier> symbols : symbolList) {
        var path = new ArrayList<>(segments);
        path.addAll(symbols);
        result.add(path);
      }
      return result;
    }
  }

  /**
   * Sequences like {@code a{0..10}} will be expanded to {@code a0, a1 ...}.
   */
  static List<ExpandedSequenceCallExpr> expandSequenceCalls(Parser parser,
                                                            List<SequenceCallExpr> calls) {
    var expandedCalls = new ArrayList<ExpandedSequenceCallExpr>(calls.size());
    for (SequenceCallExpr seqExpr : calls) {
      var targetId = seqExpr.target;
      if (targetId instanceof Identifier id && seqExpr.range == null) {
        expandedCalls.add(new ExpandedAliasDefSequenceCallExpr(
            id,
            seqExpr.loc));
      } else if (targetId instanceof CallIndexExpr callIndexExpr
          && callIndexExpr.argsIndices.size() == 1
          && callIndexExpr.argsIndices.getFirst().values.size() == 1) {
        // X(1)
        expandedCalls.add(new ExpandedSequenceCallExpr(
            callIndexExpr,
            seqExpr.loc));
      } else {
        BigInteger start = BigInteger.ZERO;
        BigInteger end = BigInteger.ZERO;
        if (seqExpr.range instanceof RangeExpr rangeExpr) {
          if (rangeExpr.from instanceof IntegerLiteral integerLiteral) {
            start = integerLiteral.number;
          } else {
            reportError(parser, "Unknown start index type " + rangeExpr.from,
                rangeExpr.from.location());
          }
          if (rangeExpr.to instanceof IntegerLiteral integerLiteral) {
            end = integerLiteral.number;
          } else {
            reportError(parser, "Unknown end index type " + rangeExpr.to,
                rangeExpr.to.location());
          }
        } else if (seqExpr.range instanceof IntegerLiteral integerLiteral) {
          start = end = integerLiteral.number;
        } else {
          reportError(parser, "Unknown index type " + seqExpr.range, requireNonNull(
              seqExpr.range).location());
        }
        for (BigInteger i = start; i.compareTo(end) <= 0; i = i.add(BigInteger.ONE)) {
          expandedCalls.add(
              new ExpandedAliasDefSequenceCallExpr(
                  new Identifier(((Identifier) targetId).name + i, seqExpr.loc),
                  seqExpr.loc));
        }
      }
    }
    return expandedCalls;
  }

  static void addDef(List<Definition> definitions, Definition def) {
    if (def instanceof DefinitionList list) {
      for (Definition item : list.items) {
        addDef(definitions, item);
      }
    } else {
      definitions.add(def);
    }
  }

  static void addEncs(List<IsEncs> encs, @Nullable IsEncs enc) {
    if (enc instanceof EncodingDefinition.EncsNode list) {
      for (IsEncs item : list.items) {
        addEncs(encs, item);
      }
    } else if (enc != null) {
      encs.add(enc);
    }
  }

  static void addStats(List<Statement> stats, Statement stmt) {
    if (stmt instanceof StatementList list) {
      for (Statement item : list.items) {
        addStats(stats, item);
      }
    } else {
      stats.add(stmt);
    }
  }

  private static void reportError(Parser parser, String error, SourceLocation location) {
    parser.diagnostics.add(Diagnostic.error(error, location).build());
  }
}
