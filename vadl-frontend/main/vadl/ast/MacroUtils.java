// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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
import static vadl.ast.ParserUtils.isDefType;
import static vadl.ast.ParserUtils.isExprType;
import static vadl.ast.ParserUtils.isStmtType;
import static vadl.error.Diagnostic.ensure;
import static vadl.error.Diagnostic.error;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.ast.nodes.AsIdExpr;
import vadl.ast.nodes.AsStrExpr;
import vadl.ast.nodes.AssemblyDefinition;
import vadl.ast.nodes.BasicSyntaxType;
import vadl.ast.nodes.CallIndexExpr;
import vadl.ast.nodes.Definition;
import vadl.ast.nodes.DefinitionList;
import vadl.ast.nodes.ExpandedAliasDefSequenceCallExpr;
import vadl.ast.nodes.ExpandedSequenceCallExpr;
import vadl.ast.nodes.Expr;
import vadl.ast.nodes.Identifier;
import vadl.ast.nodes.IdentifierOrPlaceholder;
import vadl.ast.nodes.InstructionSetDefinition;
import vadl.ast.nodes.IntegerLiteral;
import vadl.ast.nodes.IsBinOp;
import vadl.ast.nodes.IsEncs;
import vadl.ast.nodes.IsId;
import vadl.ast.nodes.MacroCall;
import vadl.ast.nodes.MacroMatch;
import vadl.ast.nodes.MacroMatchDefinition;
import vadl.ast.nodes.MacroMatchExpr;
import vadl.ast.nodes.MacroMatchNode;
import vadl.ast.nodes.MacroMatchStatement;
import vadl.ast.nodes.MacroParam;
import vadl.ast.nodes.ModelDefinition;
import vadl.ast.nodes.ModelTypeDefinition;
import vadl.ast.nodes.Node;
import vadl.ast.nodes.ProjectionType;
import vadl.ast.nodes.RangeExpr;
import vadl.ast.nodes.RecordInstance;
import vadl.ast.nodes.RecordType;
import vadl.ast.nodes.RecordTypeDefinition;
import vadl.ast.nodes.SequenceCallExpr;
import vadl.ast.nodes.Statement;
import vadl.ast.nodes.StringLiteral;
import vadl.ast.nodes.SyntaxType;
import vadl.error.Diagnostic;
import vadl.error.DiagnosticList;
import vadl.utils.Levenshtein;
import vadl.utils.SourceLocation;
import vadl.utils.WithLocation;

public class MacroUtils {

  static RecordInstance createRecordInstance(RecordType recordType, List<Node> entries,
                                             SourceLocation location) {
    if (recordType.entries.size() != entries.size()) {
      throw error("Invalid Record Invocation", location)
          .locationDescription(location, "Record `%s` expected %d arguments but got %d.",
              recordType.name, recordType.entries.size(), entries.size())
          .build();
    }
    return new RecordInstance(recordType, entries, location);
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

  // TODO: Replace by simply resolving the name to an model (either in scope)
  @Nullable
  static Node createMacroReference(Parser parser, Identifier id) {
    throw new RuntimeException("Found a reference  need");
    /*
    var model = parser.macroTable.getModel(id.name);
    if (model == null) {
      parser.diagnostics.add(
          Diagnostic.error("Unknown Model `%s`".formatted(id.name), id)
              .help("Make sure the macro is defined before (above) they are used.")
              .build());
      return null;
    } else {
      List<SyntaxType> params = new ArrayList<>(macro.params().size());
      for (MacroParam param : macro.params()) {
        params.add(param.type());
      }
      return new MacroReference(macro, new ProjectionType(params, macro.returnType()),
          id.location());
    }
    */
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
    var foundMacro = parser.macroTable.getModel(token.val);
    if (foundMacro != null) {
      parser.scanner.ResetPeek();
      if (nextToken.kind == Parser._SYM_PAREN_OPEN) {
        return foundMacro.returnType.isSubTypeOf(syntaxType);
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

  static Diagnostic tooManyMacroArgumentsError(/*@Nullable Macro macro,*/ SyntaxType type,
                                               SourceLocation location) {
    // Unfortunately, we need the types of the macro parameters to parse the invocation to
    // completion. But if more arguments are provided than parameter are defined we cannot parse
    // them and therefore only know that too many exist but not how many were provided.
    // The macro may not exist if we are calling a macro that is passed to the current macro, in
    // which case we need to rely on the type.
    var builder = error("Invalid Model Invocation", location);
//    if (macro != null) {
//      builder.locationDescription(location,
//          "Model `%s` only expected %d arguments but, you provided at least %d.",
//          macro.name().name,
//          macro.params().size(), macro.params().size() + 1);
//    } else {
      var argCount = switch (type) {
        case ProjectionType pt -> pt.arguments.size();
        default -> 1;
      };

      builder.locationDescription(location,
          "The model only expected %d arguments but, you provided at least %d.",
          argCount, argCount + 1);
    //}
    return builder.build();
  }

  static Diagnostic tooManyRecordArgumentsError(RecordType recordType, SourceLocation location) {
    // Unfortunately, we don't know how many arguments actually were provided because we cannot
    // continue parsing to find out.
    return error("Invalid Record Invocation", location)
        .locationDescription(location,
            "The record `%s` only expected %d arguments but, you provided at least %d.",
            recordType.name, recordType.entries.size(), recordType.entries.size() + 1)
        .build();
  }


  @Nullable
  private static <T> T castOrNull(Parser parser, Node node, Class<T> type, String expected) {
    if (type.isInstance(node)) {
      return type.cast(node);
    }

    if (node == null) {
      return null;
    }


      parser.diagnostics.add(
          Diagnostic.error("SyntaxType Mismatch", node)
              .description("Expected node of type `%s` but received `%s` (%s)", expected,
                  node.syntaxType().print(), node.nodeName())
              .build());

    return null;
  }

  @Nullable
  static Expr castExpr(Parser p, Node n) {
    return castOrNull(p, n, Expr.class, "Expr");
  }

  @Nullable
  static StringLiteral castForceStringLiteral(Parser p, Node n) {
    return castOrNull(p, n, StringLiteral.class, "Str");
  }

  @Nullable
  static IsEncs castEncs(Parser p, Node n) {
    return castOrNull(p, n, IsEncs.class, "Encs");
  }

  @Nullable
  static IdentifierOrPlaceholder castId(Parser p, Node n) {
    return castOrNull(p, n, IdentifierOrPlaceholder.class, "Id");
  }

  @Nullable
  static IsBinOp castBinOp(Parser p, Node n) {
    return castOrNull(p, n, IsBinOp.class, "BinOp");
  }

  /**
   * Casts a node to either IsBinOp or IsId, useful in macro contexts where either type might be
   * valid.
   * This is similar to {@link #castBinOp} but also accepts identifier nodes.
   *
   * @param p The parser instance for error reporting
   * @param n The node to cast
   * @return The node if it's IsBinOp or IsId, otherwise null with an error reported
   */
  @Nullable
  static Node castBinOpOrId(Parser p, Node n) {
    if (n instanceof IsBinOp || n instanceof IsId) {
      return n;
    }

    String message;
//    if (isPlaceholder(n)) {
//      var sb = new StringBuilder("");
//      n.prettyPrint(0, sb);
//      var name = sb.toString();
//
//      p.diagnostics.add(
//          Diagnostic.error("Unknown Model `%s`".formatted(name), n)
//              .help("Make sure the macro is defined before (above) they are used.")
//              .build());
//    } else {
      message =
          "Expected node of type BinOp or Id, received "
              + n.syntaxType().print() + " - " + n;
      p.diagnostics.add(
          Diagnostic.error("SyntaxType Mismatch", n)
              .description("%s", message)
              .build());
//    }

    return null;
  }

    @Nullable
  static Definition castCommonDef(Parser p, Node n) {
    if (n instanceof Definition d && d.syntaxType().isSubTypeOf(BasicSyntaxType.COMMON_DEFS)) {
      return d;
    }

    // Only here is a special syntax type check required because the syntax type
    // Isa Def is not allowed here but doesn't really exist in the AST and therefore castOrNull
    // doesn't really handle them.
    var casted = castOrNull(p, n, Definition.class,  "CommonDefs");
    if (casted != null && !casted.syntaxType().isSubTypeOf(BasicSyntaxType.COMMON_DEFS)) {
      var message =
          "Expected node of type Defs, received "
              + casted.syntaxType().print();
      p.diagnostics.add(
          Diagnostic.error("SyntaxType Mismatch", n)
              .description("%s", message)
              .build());
    }
    return casted;
  }

  @Nullable
  static Definition castIsaDef(Parser p, Node n) {
    return (n instanceof Definition d && d.syntaxType().isSubTypeOf(BasicSyntaxType.ISA_DEFS))
        ? d : castOrNull(p, n, Definition.class, "IsaDefs");
  }

  @Nullable
  static Statement castStat(Parser p, Node n) {
    return castOrNull(p, n, Statement.class, "Stat");
  }

  @Nullable
  static Statement castStats(Parser p, Node n) {
    return castOrNull(p, n, Statement.class, "Stats");
  }


  @Nullable
  static Node expandTopLevelMacroCall(Parser parser, MacroCall macroCall) {
    // Resolve the model
    ensure(macroCall.subcalls().isEmpty(), () -> error("Invalid model call", macroCall));
    var model = parser.macroTable.getModel(macroCall.name().name);
    if (model == null) {
      throw new IllegalStateException("Invalid model call: " + macroCall);
    }

    // TODO don't evaluate if broken

    try {
      return MacroExpander.expandMacroCall(model, macroCall, parser.macroOverrides);
    } catch (DiagnosticList e) {
      parser.diagnostics.addAll(e.items);
      return null;
    }
  }

  @Nullable
  static Node expandTopLevelAsId(Parser parser, AsIdExpr asId) {
    try {
      return MacroExpander.expandAsId(asId);
    } catch (DiagnosticList e) {
      parser.diagnostics.addAll(e.items);
      return null;
    }
  }

  @Nullable
  static Node expandTopLevelAsStr(Parser parser, AsStrExpr asStr) {
    try {
      return MacroExpander.expandAsStr(asStr);
    } catch (DiagnosticList e) {
      parser.diagnostics.addAll(e.items);
      return null;
    }
  }


  /**
   * Assembly definitions can be written with multiple identifiers to be bound to multiple
   * (pseudo) instructions. However, for correct further processing they need to be expanded into
   * multiple definitions.
   * The input requires a linked list because otherwise performance would degredate.
   *
   * @param isaDefs to be expanded.
   * @return returns the original isaDefs with the Assemblies expanded.
   */
  @SuppressWarnings("NonApiType")
  static LinkedList<Definition> expandAssemblyDefinitionsInIsa(Parser parser,
                                                               LinkedList<Definition> isaDefs) {
    for (var iter = isaDefs.listIterator(); iter.hasNext(); ) {
      var def = iter.next();
      if (!(def instanceof AssemblyDefinition assembly) || assembly.identifiers.size() == 1) {
        continue;
      }

      var expanded = MacroExpander.expandAssemblyDefs(assembly);
      iter.set(expanded.getFirst());
      expanded.subList(1, expanded.size()).forEach(iter::add);
    }

    return isaDefs;
  }

  /**
   * Defines all provided macro definitions into the provided macroTable.
   * This is mostly useful because macros ca generate macros and these generated macros need to be
   * injected into the symbol table to be accessable.
   *
   * @param macroTable  to be modified.
   * @param definitions to be inserted.
   */
  static void defineGeneratedMacroSymbols(SymbolTable macroTable, Definition definition) {
      if (definition instanceof DefinitionList list) {
        defineGeneratedMacroSymbols(macroTable, list.items);
      } else if (definition instanceof ModelDefinition modelDefinition) {
        macroTable.defineSymbol(modelDefinition);
      } else if (definition instanceof ModelTypeDefinition modelTypeDefinition) {
        macroTable.defineSymbol(modelTypeDefinition);
      } else if (definition instanceof RecordTypeDefinition recordTypeDefinition) {
        macroTable.defineSymbol(recordTypeDefinition);
      }
  }

  /**
   * Defines all provided macro definitions into the provided macroTable.
   * This is mostly useful because macros ca generate macros and these generated macros need to be
   * injected into the symbol table to be accessable.
   *
   * @param macroTable  to be modified.
   * @param definitions to be inserted.
   */
  static void defineGeneratedMacroSymbols(SymbolTable macroTable, List<Definition> definitions) {
    for (Definition definition : definitions) {
      defineGeneratedMacroSymbols(macroTable, definition);
    }
  }

  /**
   * Recursively reads all macros in each of the extending instruction definitions.
   *
   * @param macroTable the macro table that should be fed with found macro symbols
   * @param isa        the isa that should be (recursively) traversed to find all macro definitions.
   */
  static void defineGeneratedMacroSymbols(SymbolTable macroTable, InstructionSetDefinition isa) {
    defineGeneratedMacroSymbols(macroTable, isa.definitions);
    // FIXME: This is not optimal as we traverse an ISA potentially multiple times.
    // as we don't have access to the macroTable of the referenced ISA, we must
    // do the traversal again.
    for (IsId extending : isa.extending) {
      var extendingIsa = (InstructionSetDefinition) requireNonNull(extending.target());
      defineGeneratedMacroSymbols(macroTable, extendingIsa);
    }
  }

  // If this model is overwritten then we need to verify that the return type is a `Id` as those
  // are the only ones that are allowed to overriden.
  static void verifyModelOverrideType(Parser parser, ModelDefinition def,
                                         SourceLocation location) {
    if (def.id != null
        && parser.macroOverrides.containsKey(def.id.pathToString())
        && !def.returnType.equals(BasicSyntaxType.ID)) {
      parser.diagnostics.add(
          error("Invalid Model Override", location)
              .locationNote(location,
                  "This model was overridden but only models returning `Id` can be overridden.")
              .build()
      );
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

  /**
   * Returns the macroType of anything that can be evaluated by the macro system.
   * Most usually these are macros or parameters of the currently parsed macro or one of the
   * enclosing macros for macro-in-macro cases.
   *
   * @param parser that is active.
   * @param name to be resolved.
   * @return the type if found, null otherwise.
   */
  @Nullable
  static SyntaxType macroCallableType(Parser parser, Identifier name) {
    // Search the symbol table
    var model = parser.macroTable.getModel(name.name);
    if (model != null) {
      return model.projectionType();
    }

    // Search the enclosing params
    for (var modelParams : parser.macroContext) {
      for (var param : modelParams) {
        if (param.name().name.equals(name.name)) {
          return param.type();
        }
      }
    }

    // Error out and return null
    // FIXME: Add suggestions
    parser.diagnostics.add(
        error("Unknown Macro: %s".formatted(name.name), name)
            .build()
    );

    return null;
  }

  static boolean isInsideModel(Parser parser) {
      return !parser.macroContext.isEmpty();
  }

  static void addSyntaxTypeError(Parser parser, SyntaxType required, SyntaxType actual, WithLocation loc) {
    parser.diagnostics.add(
        Diagnostic.error("SyntaxType Mismatch", loc)
            .description("Required `%s`, but got `%s`", required, actual)
            .build());
  }

  private static void reportError(Parser parser, String error, SourceLocation location) {
    parser.diagnostics.add(Diagnostic.error(error, location).build());
  }
}
