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
import static vadl.ast.ParserUtils.incrementFollowupErrors;
import static vadl.ast.ParserUtils.isDefType;
import static vadl.ast.ParserUtils.isExprType;
import static vadl.ast.ParserUtils.isStmtType;
import static vadl.error.Diagnostic.ensure;
import static vadl.error.Diagnostic.error;

import com.google.common.collect.Streams;
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
import vadl.ast.nodes.Identifier;
import vadl.ast.nodes.InstructionSetDefinition;
import vadl.ast.nodes.IntegerLiteral;
import vadl.ast.nodes.IsId;
import vadl.ast.nodes.MacroCall;
import vadl.ast.nodes.MacroMatch;
import vadl.ast.nodes.MacroMatchDefinition;
import vadl.ast.nodes.MacroMatchExpr;
import vadl.ast.nodes.MacroMatchNode;
import vadl.ast.nodes.MacroMatchStatement;
import vadl.ast.nodes.MacroParam;
import vadl.ast.nodes.MacroReference;
import vadl.ast.nodes.ModelDefinition;
import vadl.ast.nodes.ModelTypeDefinition;
import vadl.ast.nodes.Node;
import vadl.ast.nodes.ProjectionType;
import vadl.ast.nodes.RangeExpr;
import vadl.ast.nodes.RecordInstance;
import vadl.ast.nodes.RecordType;
import vadl.ast.nodes.RecordTypeDefinition;
import vadl.ast.nodes.SequenceCallExpr;
import vadl.ast.nodes.SyntaxType;
import vadl.error.Diagnostic;
import vadl.error.DiagnosticList;
import vadl.utils.Levenshtein;
import vadl.utils.SourceLocation;
import vadl.utils.WithLocation;

/**
 * A collection of static methods that help the parser to deal with macros.
 * This is a semantic extension of {@link ParserUtils}.
 */
class MacroUtils {

  @Nullable
  static RecordInstance createRecordInstance(Parser parser, RecordType recordType,
                                             List<Node> entries,
                                             SourceLocation location) {
    if (recordType.entries.size() != entries.size()) {
      parser.diagnostics.add(error("Invalid Record Invocation", location)
          .locationDescription(location, "Record `%s` expected %d arguments but got %d.",
              recordType.name, recordType.entries.size(), entries.size())
          .build());
      return null;
    }

    // Already an argument is broken return null
    if (entries.stream().anyMatch(e -> e == null)) {
      incrementFollowupErrors(parser);
      return null;
    }

    return new RecordInstance(recordType, entries, location);
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

  /**
   * Create a reference for a model, see {@link MacroReference}.
   * The model cannot be resolved an error is added to the parser.
   *
   * @param parser that is active.
   * @param id that referenced it.
   * @return the macro reference if a model is found, null otherwise
   */
  @Nullable
  static MacroReference createMacroReference(Parser parser, Identifier id) {
    // Cannot directly return the model because the syntax type wouldn't be correct.
    var model = parser.macroTable.getModel(id.name);
    if (model == null) {
      parser.diagnostics.add(unknownModelError(parser, id.name, id));
      return null;
    }
    return new MacroReference(model);
  }

  @Nullable
  static Node expandTopLevelMacroCall(Parser parser, MacroCall macroCall) {
    // Resolve the model
    ensure(macroCall.subcalls().isEmpty(), () -> error("Invalid model call", macroCall));
    var model = parser.macroTable.getModel(macroCall.name().name);
    if (model == null) {
      throw new IllegalStateException("Invalid model call: " + macroCall);
    }

    // Stop expansion if broken.
    if (!model.syntaxType().isValid()) {
      incrementFollowupErrors(parser);
      return null;
    }

    try {
      return parser.ast.withPassTiming("Macro Expanding",
          () -> MacroExpander.expandMacroCall(model, macroCall));
    } catch (DiagnosticList e) {
      parser.diagnostics.addAll(e.items);
      return null;
    }
  }

  @Nullable
  static Node expandTopLevelAsId(Parser parser, AsIdExpr asId) {
    try {
      return parser.ast.withPassTiming("Macro Expanding", () -> MacroExpander.expandAsId(asId));
    } catch (DiagnosticList e) {
      parser.diagnostics.addAll(e.items);
      return null;
    }
  }

  @Nullable
  static Node expandTopLevelAsStr(Parser parser, AsStrExpr asStr) {
    try {
      return parser.ast.withPassTiming("Macro Expanding", () -> MacroExpander.expandAsStr(asStr));
    } catch (DiagnosticList e) {
      parser.diagnostics.addAll(e.items);
      return null;
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

      var expanded = parser.ast.withPassTiming("Macro Expanding",
          () -> MacroExpander.expandAssemblyDefs(assembly));
      iter.set(expanded.getFirst());
      expanded.subList(1, expanded.size()).forEach(iter::add);
    }

    return isaDefs;
  }

  // FIXME: DO wee need that?
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

  /**
   * Returns the macroType of anything that can be evaluated by the macro system.
   * Most usually these are macros or parameters of the currently parsed macro or one of the
   * enclosing macros for macro-in-macro cases.
   *
   * @param parser that is active.
   * @param name   to be resolved.
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
    parser.diagnostics.add(unknownModelError(parser, name.name, name));
    return null;
  }

  /**
   * Used to determine if the parser is currently inside one or more model definitions.
   *
   * @param parser that is active
   * @return true if inside, false otherwise.
   */
  static boolean isInsideModel(Parser parser) {
    return !parser.macroContext.isEmpty();
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

  /**
   * Return the syntax type of the macromatch by looking ahead of the syntax tokens.
   * The peek will be reset afterwards.
   *
   * @param parser at which state the macro match type should be calculated.
   * @return the type of the match.
   */
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

  /**
   * Casts a node to either IsBinOp or IsId, useful in macro contexts where either type might be
   * valid.
   *
   * @param parser The parser instance for error reporting
   * @param node   The node to cast
   * @return The node if it's IsBinOp or IsId, otherwise null with an error reported
   */
  @Nullable
  static Node castBinOpOrId(Parser parser, Node node) {
    if (node.syntaxType().isSubTypeOf(BasicSyntaxType.BIN_OP) || node.syntaxType()
        .isSubTypeOf(BasicSyntaxType.STR)) {
      return node;
    }

    parser.diagnostics.add(
        Diagnostic.error("SyntaxType Mismatch", node)
            .description("Expected node of type `BinOp` or `Id` but received `%s`",
                node.syntaxType().print())
            .build());

    return null;
  }

  /**
   * Defines all provided macro definitions into the provided macroTable.
   * This is mostly useful because macros ca generate macros and these generated macros need to be
   * injected into the symbol table to be accessable.
   *
   * @param macroTable  to be modified.
   * @param definition to be inserted.
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

  /**
   * If this model is overwritten then we need to verify that the return type is a `Id` as those
   * are the only ones that are allowed to overriden.
   *
   * @param parser that is currently active.
   * @param def of the model to be checked.
   * @param signatureLoc of the models signature.
   */
  static void verifyModelOverrideType(Parser parser, ModelDefinition def,
                                      SourceLocation signatureLoc) {
    if (def.id != null
        && parser.macroOverrides.containsKey(def.id.pathToString())
        && !def.returnType.equals(BasicSyntaxType.ID)) {
      parser.diagnostics.add(
          error("Invalid Model Override", signatureLoc)
              .locationNote(signatureLoc,
                  "This model was overridden but only models returning `Id` can be overridden.")
              .build()
      );
    }
  }

  static Diagnostic tooManyMacroArgumentsError(SyntaxType type, SourceLocation location) {
    var argCount = switch (type) {
      case ProjectionType pt -> pt.arguments.size();
      default -> 1;
    };

    return error("Invalid Model Invocation", location)
        .locationDescription(location,
            "The model only expected %d arguments but, you provided at least %d.", argCount,
            argCount + 1)
        .build();
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

  static Diagnostic tooFewMacroArgumentsError(int expected, int actual, SourceLocation location) {
    return error("Invalid Model Invocation", location)
        .locationDescription(location, "The model expected %d arguments but, %d were provided.",
            expected, actual)
        .build();
  }

  static Diagnostic unknownModelError(Parser parser, String name, WithLocation location) {
    var candidates =
        Streams.concat(
                parser.macroContext.stream()
                    .flatMap(l -> l.stream())
                    .map(p -> p.name().name),
                parser.macroTable.allMacroSymbolNamesOf(ModelDefinition.class).stream()
            )
            .toList();

    return error("Unknown Model: `%s`".formatted(name), location)
        .help("Make sure the model is defined before (above) it is used.")
        .suggestions(Levenshtein.suggestions(name, candidates))
        .build();
  }

  static boolean assertSyntaxType(Parser parser, @Nullable Node node, SyntaxType requiredType) {
    if (requiredType == BasicSyntaxType.INVALID) {
      // This only happens because of a followup error that already got reported, no need to report
      // another diagnostic.
      return false;
    }

    if (node != null && !node.syntaxType().isSubTypeOf(requiredType)) {
      if (!node.syntaxType().isValid()) {
        incrementFollowupErrors(parser);
        return false;
      }

      addSyntaxTypeError(parser, requiredType, node, node);
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

  static void addSyntaxTypeError(Parser parser, SyntaxType required, SyntaxType actual,
                                 WithLocation loc) {
    if (!actual.isValid()) {
      // This is a follow-up error, no need to report it
      incrementFollowupErrors(parser);
      return;
    }

    parser.diagnostics.add(
        Diagnostic.error("SyntaxType Mismatch", loc)
            .description("Expected node of type `%s` but received `%s`", required.print(),
                actual.print())
            .build());
  }

  static void addSyntaxTypeError(Parser parser, SyntaxType required, Node actual,
                                 WithLocation loc) {
    if (!actual.syntaxType().isValid()) {
      // This is a follow-up error, no need to report it
      incrementFollowupErrors(parser);
      return;
    }

    parser.diagnostics.add(
        Diagnostic.error("SyntaxType Mismatch", loc)
            .description("Expected node of type `%s` but received `%s` (%s)", required.print(),
                actual.syntaxType().print(), actual.nodeName())
            .build());
  }

  private static void reportError(Parser parser, String error, SourceLocation location) {
    parser.diagnostics.add(Diagnostic.error(error, location).build());
  }
}
