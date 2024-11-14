package vadl.ast;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.utils.SourceLocation;

class ParserUtils {

  static String VADL_EXTENSION = ".vadl";

  static boolean[] NO_OPS;
  static boolean[] BIN_OPS;
  static boolean[] BIN_OPS_EXCEPT_GT;
  static boolean[] BIN_OPS_EXCEPT_IN;
  static boolean[] UN_OPS;

  // Must be kept in sync with allowedIdentifierKeywords
  static boolean[] ID_TOKENS;

  // Must be kept in sync with auxiliaryFields
  static boolean[] AUX_FIELD_TOKENS;

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
    ID_TOKENS[Parser._APPEND] = true;
    ID_TOKENS[Parser._BINARY] = true;
    ID_TOKENS[Parser._CALL] = true;
    ID_TOKENS[Parser._CONSTANT] = true;
    ID_TOKENS[Parser._ENCODE] = true;
    ID_TOKENS[Parser._EXCEPTION] = true;
    ID_TOKENS[Parser._FETCH] = true;
    ID_TOKENS[Parser._FIRMWARE] = true;
    ID_TOKENS[Parser._GROUP] = true;
    ID_TOKENS[Parser._INSTRUCTION] = true;
    ID_TOKENS[Parser._MEMORY] = true;
    ID_TOKENS[Parser._NONE] = true;
    ID_TOKENS[Parser._NOP] = true;
    ID_TOKENS[Parser._OPERATION] = true;
    ID_TOKENS[Parser._PREDICATE] = true;
    ID_TOKENS[Parser._PREDICTION] = true;
    ID_TOKENS[Parser._READ] = true;
    ID_TOKENS[Parser._REGISTER] = true;
    ID_TOKENS[Parser._RETURN] = true;
    ID_TOKENS[Parser._SEQUENCE] = true;
    ID_TOKENS[Parser._STAGE] = true;
    ID_TOKENS[Parser._START] = true;
    ID_TOKENS[Parser._STARTUP] = true;
    ID_TOKENS[Parser._STOP] = true;
    ID_TOKENS[Parser._SYM_IN] = true;
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
    ID_TOKENS[Parser._WRITE] = true;

    AUX_FIELD_TOKENS = NO_OPS.clone();
    AUX_FIELD_TOKENS[Parser._PREDICATE] = true;
    AUX_FIELD_TOKENS[Parser._ENCODE] = true;
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
      reportError(parser, "Unknown model: " + id.name, id.location());
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
    if (node != null && !node.syntaxType().isSubTypeOf(requiredType)) {
      parser.errors.SemErr(parser.t.line, parser.t.col,
          message + ": Required %s, node is %s".formatted(requiredType, node.syntaxType()));
      return false;
    }
    return true;
  }

  /**
   * Casts the node to the type Expr, or reports an error and returns a dummy node of that type.
   * Useful in the parser, where throwing cast exceptions is not the best way of error reporting.
   */
  static Expr castExpr(Parser parser, Node node) {
    if (node instanceof Expr expr) {
      return expr;
    } else {
      parser.errors.SemErr(node.location().begin().line(), node.location().begin().column(),
          "Expected node of type Ex, received " + node.syntaxType().print() + " - " + node);
      return DUMMY_EXPR;
    }
  }

  /**
   * Casts the node to the type Encs, or reports an error and returns a dummy node of that type.
   * Useful in the parser, where throwing cast exceptions is not the best way of error reporting.
   */
  static IsEncs castEncs(Parser parser, Node node) {
    if (node instanceof IsEncs encs) {
      return encs;
    } else {
      parser.errors.SemErr(node.location().begin().line(), node.location().begin().column(),
          "Expected node of type Encs, received " + node.syntaxType().print() + " - " + node);
      return new EncodingDefinition.EncodingField(DUMMY_ID, DUMMY_ID);
    }
  }

  /**
   * Casts the node to the type Id, or reports an error and returns a dummy node of that type.
   * Useful in the parser, where throwing cast exceptions is not the best way of error reporting.
   */
  static IdentifierOrPlaceholder castId(Parser parser, Node node) {
    if (node instanceof IdentifierOrPlaceholder identifierOrPlaceholder) {
      return identifierOrPlaceholder;
    } else {
      parser.errors.SemErr(node.location().begin().line(), node.location().begin().column(),
          "Expected node of type Id, received " + node.syntaxType().print() + " - " + node);
      return DUMMY_ID;
    }
  }

  /**
   * Casts the node to the type BinOp, or reports an error and returns a dummy node of that type.
   * Useful in the parser, where throwing cast exceptions is not the best way of error reporting.
   */
  static IsBinOp castBinOp(Parser parser, Node node) {
    if (node instanceof IsBinOp isBinOp) {
      return isBinOp;
    } else {
      parser.errors.SemErr(node.location().begin().line(), node.location().begin().column(),
          "Expected node of type BinOp, received " + node.syntaxType().print() + " - " + node);
      return new BinOp(Operator.Xor(), node.location());
    }
  }

  /**
   * Casts the node to the type IsaDefs, or reports an error and returns a dummy node of that type.
   * Useful in the parser, where throwing cast exceptions is not the best way of error reporting.
   */
  static Definition castCommonDef(Parser parser, Node node) {
    if (node instanceof Definition definition
        && definition.syntaxType().isSubTypeOf(BasicSyntaxType.COMMON_DEFS)) {
      return definition;
    } else {
      parser.errors.SemErr(node.location().begin().line(), node.location().begin().column(),
          "Expected node of type CommonDefs, received " + node.syntaxType().print() + " - " + node);
      return DUMMY_DEF;
    }
  }

  /**
   * Casts the node to the type IsaDefs, or reports an error and returns a dummy node of that type.
   * Useful in the parser, where throwing cast exceptions is not the best way of error reporting.
   */
  static Definition castIsaDef(Parser parser, Node node) {
    if (node instanceof Definition definition
        && definition.syntaxType().isSubTypeOf(BasicSyntaxType.ISA_DEFS)) {
      return definition;
    } else {
      parser.errors.SemErr(node.location().begin().line(), node.location().begin().column(),
          "Expected node of type IsaDefs, received " + node.syntaxType().print() + " - " + node);
      return DUMMY_DEF;
    }
  }

  /**
   * Casts the node to the type Stat, or reports an error and returns a dummy node of that type.
   * Useful in the parser, where throwing cast exceptions is not the best way of error reporting.
   */
  static Statement castStat(Parser parser, Node node) {
    if (node instanceof Statement statement) {
      return statement;
    } else {
      parser.errors.SemErr(node.location().begin().line(), node.location().begin().column(),
          "Expected node of type Stat, received " + node.syntaxType().print() + " - " + node);
      return DUMMY_STAT;
    }
  }

  /**
   * Casts the node to the type Stats, or reports an error and returns a dummy node of that type.
   * Useful in the parser, where throwing cast exceptions is not the best way of error reporting.
   */
  static Statement castStats(Parser parser, Node node) {
    if (node instanceof Statement statement) {
      return statement;
    } else {
      parser.errors.SemErr(node.location().begin().line(), node.location().begin().column(),
          "Expected node of type Stats, received " + node.syntaxType().print() + " - " + node);
      return DUMMY_STAT;
    }
  }

  static Node expandNode(Parser parser, Node node) {
    var macroExpander = new MacroExpander(Map.of(), parser.macroOverrides, node.location());
    var expanded = macroExpander.expandNode(node);
    if (parser.macroContext.isEmpty()) {
      // TODO This is necessary to completely copy all nodes to not cause issues
      //  in symbol collection - find out why
      return macroExpander.expandNode(expanded);
    }
    return expanded;
  }

  static void readMacroSymbols(SymbolTable macroTable, List<Definition> definitions) {
    for (Definition definition : definitions) {
      if (definition instanceof DefinitionList list) {
        readMacroSymbols(macroTable, list.items);
      } else if (definition instanceof ModelDefinition modelDefinition) {
        macroTable.addModelDefinition(modelDefinition);
      }
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
        ? resolveUri(parser, Objects.requireNonNull(fileId)) : resolveUri(parser, filePath.value);
    if (modulePath != null) {
      var macroOverrides = new HashMap<String, String>();
      for (StringLiteral arg : args) {
        var keyValue = arg.value.split("=", 2);
        macroOverrides.put(keyValue[0], keyValue[1]);
      }
      try {
        var ast = VadlParser.parse(modulePath, macroOverrides);
        parser.macroTable.importFrom(ast, importedSymbols);
        return new ImportDefinition(ast, importedSymbols, fileId, filePath, args, loc);
      } catch (Exception e) {
        parser.errors.SemErr("Error during module evaluation - " + e);
      }
    }
    return new ConstantDefinition(new Identifier("invalid", parser.loc()), null,
        new Identifier("invalid", parser.loc()), parser.loc());
  }

  static @Nullable Path resolveUri(Parser parser, IsId importPath) {
    if (importPath instanceof Identifier id) {
      return resolveUri(parser, id.name);
    } else if (importPath instanceof IdentifierPath identifierPath) {
      return resolveUri(parser, ((Identifier) identifierPath.segments.get(0)).name);
    } else {
      parser.errors.SemErr("Could not resolve module path: " + importPath);
      return null;
    }
  }

  static @Nullable Path resolveUri(Parser parser, String name) {
    var resolutionUri = Objects.requireNonNullElse(parser.resolutionUri, parser.sourceFile);
    var relativeToSpec = Paths.get(resolutionUri.resolve(name));
    if (Files.isRegularFile(relativeToSpec)) {
      return relativeToSpec;
    }
    var withAppendedExtension = relativeToSpec.resolveSibling(
        relativeToSpec.getFileName() + VADL_EXTENSION
    );
    if (Files.isRegularFile(withAppendedExtension)) {
      return withAppendedExtension;
    }
    parser.errors.SemErr("Could not resolve module path: " + name);
    return null;
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

  static List<SequenceCallExpr> expandSequenceCalls(Parser parser, List<SequenceCallExpr> calls) {
    var expandedCalls = new ArrayList<SequenceCallExpr>(calls.size());
    for (SequenceCallExpr callExpr : calls) {
      if (callExpr.range == null) {
        expandedCalls.add(callExpr);
      } else {
        BigInteger start = BigInteger.ZERO;
        BigInteger end = BigInteger.ZERO;
        if (callExpr.range instanceof RangeExpr rangeExpr) {
          if (rangeExpr.from instanceof IntegerLiteral integerLiteral) {
            start = integerLiteral.number;
          } else {
            reportError(parser, "Unknown start index type " + rangeExpr.from,
                rangeExpr.from.location());
          }
          if (rangeExpr.to instanceof IntegerLiteral integerLiteral) {
            end = integerLiteral.number;
          } else {
            reportError(parser, "Unknown start index type " + rangeExpr.to,
                rangeExpr.to.location());
          }
        } else if (callExpr.range instanceof IntegerLiteral integerLiteral) {
          start = end = integerLiteral.number;
        } else {
          reportError(parser, "Unknown index type " + callExpr.range, callExpr.range.location());
        }
        for (; !start.equals(end); start = start.add(BigInteger.valueOf(end.compareTo(start)))) {
          expandedCalls.add(
              new SequenceCallExpr(new Identifier(callExpr.target.name + start, callExpr.loc), null,
                  callExpr.loc));
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
    parser.errors.SemErr(location.begin().line(), location.begin().column(), error);
  }
}
