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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import vadl.ast.nodes.AsmGrammarAlternativesDefinition;
import vadl.ast.nodes.AsmGrammarElementDefinition;
import vadl.ast.nodes.AsmGrammarLiteralDefinition;
import vadl.ast.nodes.AsmGrammarRuleDefinition;
import vadl.ast.nodes.AsmGrammarTypeDefinition;
import vadl.ast.nodes.FunctionDefinition;
import vadl.ast.nodes.Identifier;
import vadl.ast.nodes.IntegerLiteral;
import vadl.ast.nodes.Parameter;
import vadl.ast.nodes.StringLiteral;
import vadl.ast.nodes.TypeLiteral;
import vadl.ast.nodes.UnOp;
import vadl.ast.nodes.UnaryExpr;
import vadl.ast.nodes.UnaryOperator;
import vadl.types.asmTypes.AsmType;
import vadl.types.asmTypes.ConstantAsmType;
import vadl.types.asmTypes.ExpressionAsmType;
import vadl.types.asmTypes.InstructionAsmType;
import vadl.types.asmTypes.OperandAsmType;
import vadl.types.asmTypes.RegisterAsmType;
import vadl.types.asmTypes.StringAsmType;
import vadl.types.asmTypes.SymbolAsmType;
import vadl.types.asmTypes.VoidAsmType;
import vadl.utils.SourceLocation;
import vadl.utils.WithLocation;
import vadl.viam.asm.AsmGrammarDefaultTerminalRules;

/**
 * Defines default rules for the assembly grammar.
 * <p>
 * Provides a method {@link AsmGrammarDefaultRules#notIncludedDefaultRules}
 * to get default rules that are not included in the user defined rules.
 * </p>
 */
public class AsmGrammarDefaultRules {

  /**
   * The name of the built-in function that negates a 64-bit integer.
   */
  public static final String BUILTIN_ASM_NEG = "VADL_asmparser_neg";

  private static final HashMap<AsmGrammarRuleDefinition, Pattern> patternCache = new HashMap<>();

  /**
   * Returns a list of default rules that are not included in the given rules.
   *
   * @param rules the rules to check against the default rules
   * @return a list of default rules that are not included in the given rules
   */
  public static List<AsmGrammarRuleDefinition> notIncludedDefaultRules(
      List<AsmGrammarRuleDefinition> rules) {
    return defaultRules().stream().filter(
        defaultRule -> rules.stream().noneMatch(r -> r.id.name.equals(defaultRule.id.name))
    ).toList();
  }

  /**
   * Get the precompiled regex patterns for terminal rules.
   * The regex patterns are used to check for LL(1) conflicts in the Asm Grammar.
   *
   * @return a map of terminal rules and their corresponding regex patterns
   */
  public static Map<AsmGrammarRuleDefinition, Pattern> terminalRuleRegexPatterns() {
    return patternCache;
  }

  /**
   * Get the name of the terminal rule that matches the given parse value.
   *
   * @param parseValue the parse value to match
   * @return the name of the matching terminal rule, or null if no match is found
   */
  @Nullable
  public static String getMatchingTerminalRule(String parseValue) {
    return AsmGrammarDefaultTerminalRules.getMatchingTerminalRule(parseValue);
  }

  private static List<AsmGrammarRuleDefinition> defaultRules() {
    // regex pattern needed for checking LL(1) conflicts
    // in the AsmParser this is handled by the LLVM lexer
    var rules = new ArrayList<>(defaultTerminalRules());
    rules.addAll(List.of(
        nonTerminalRule("Statement", InstructionAsmType.instance(), false, instructionRule(),
            ruleReference("EOL", VoidAsmType.instance())),
        nonTerminalRule("Register", RegisterAsmType.instance(),
            false, ruleReference("IDENTIFIER", StringAsmType.instance())),
        nonTerminalRule("ImmediateOperand", OperandAsmType.instance(), false,
            ruleReference("Expression")),
        nonTerminalRule("Identifier", null, false, ruleReference("IDENTIFIER")),
        nonTerminalRule("Expression", ExpressionAsmType.instance(), true,
            ruleReference("Expression")),
        nonTerminalRule("Instruction", InstructionAsmType.instance(), true,
            ruleReference("Instruction")),
        integerRule(),
        nonTerminalRule("Natural", ConstantAsmType.instance(), false,
            ruleReference("INTEGER", ConstantAsmType.instance())),
        nonTerminalRule("Label", SymbolAsmType.instance(), false,
            ruleReference("Identifier", StringAsmType.instance()))
    ));
    return rules;
  }

  /**
   * Defines a function that negates a 64-bit integer.
   * This function is used in the default grammar rules to be able to parse negative constants.
   *
   * @return a function definition that negates a 64-bit integer
   */
  public static FunctionDefinition asmNegFunctionDefinition(WithLocation locatable) {
    var loc = locatable.location();
    return new FunctionDefinition(
        new Identifier(BUILTIN_ASM_NEG, loc),
        new ArrayList<>(List.of(
            new Parameter(
                new Identifier("x", loc),
                new TypeLiteral(new Identifier("SInt", loc),
                    List.of(new IntegerLiteral(64, loc)),
                    loc)
            )
        )),
        new TypeLiteral(new Identifier("SInt", loc),
            List.of(new IntegerLiteral(64, loc)),
            loc),
        new UnaryExpr(new UnOp(UnaryOperator.NEGATIVE, loc),
            new Identifier("x", loc)),
        loc
    );
  }

  private static List<AsmGrammarRuleDefinition> defaultTerminalRules() {
    return AsmGrammarDefaultTerminalRules.RULES.stream()
        .map(rule -> terminalRule(rule.name(), rule.regularExpression(), rule.escapeRegex(),
            terminalRuleType(rule.name())))
        .toList();
  }

  private static AsmType terminalRuleType(String name) {
    return switch (name) {
      case "INTEGER" -> ConstantAsmType.instance();
      case "EOL" -> VoidAsmType.instance();
      default -> StringAsmType.instance();
    };
  }

  private static AsmGrammarRuleDefinition terminalRule(String name, String regularExpression,
                                                       boolean escapeRegex,
                                                       AsmType terminalRuleType) {
    var asmTypeDef = new AsmGrammarTypeDefinition(
        new Identifier(terminalRuleType.name(),
            SourceLocation.INVALID_SOURCE_LOCATION),
        SourceLocation.INVALID_SOURCE_LOCATION
    );

    var grammarLiteral = new AsmGrammarLiteralDefinition(
        null, new ArrayList<>(), new StringLiteral(regularExpression), asmTypeDef,
        SourceLocation.INVALID_SOURCE_LOCATION
    );
    grammarLiteral.asmType = terminalRuleType;

    var rule = new AsmGrammarRuleDefinition(
        new Identifier(name, SourceLocation.INVALID_SOURCE_LOCATION),
        null,
        new AsmGrammarAlternativesDefinition(
            new ArrayList<>(List.of(List.of(
                new AsmGrammarElementDefinition(
                    null, null, false, grammarLiteral, null, null,
                    null, null, null, SourceLocation.INVALID_SOURCE_LOCATION
                )
            ))),
            SourceLocation.INVALID_SOURCE_LOCATION
        ),
        SourceLocation.INVALID_SOURCE_LOCATION
    );
    rule.isTerminalRule = true;

    var pattern =
        Pattern.compile(escapeRegex ? Pattern.quote(regularExpression) : regularExpression);
    patternCache.put(rule, pattern);
    return rule;
  }

  private static AsmGrammarRuleDefinition nonTerminalRule(String name, @Nullable AsmType ruleType,
                                                          boolean isBuiltinRule,
                                                          AsmGrammarElementDefinition... elements) {
    var alternativesDefinition = new AsmGrammarAlternativesDefinition(
        new ArrayList<>(List.of(List.of(elements))),
        SourceLocation.INVALID_SOURCE_LOCATION
    );
    alternativesDefinition.asmType = ruleType;

    AsmGrammarTypeDefinition typeDef = null;
    if (ruleType != null) {
      typeDef = new AsmGrammarTypeDefinition(new Identifier(ruleType.name(),
          SourceLocation.INVALID_SOURCE_LOCATION),
          SourceLocation.INVALID_SOURCE_LOCATION);
    }

    var rule = new AsmGrammarRuleDefinition(
        new Identifier(name, SourceLocation.INVALID_SOURCE_LOCATION), typeDef,
        alternativesDefinition, SourceLocation.INVALID_SOURCE_LOCATION
    );

    rule.isBuiltinRule = isBuiltinRule;
    return rule;
  }

  private static AsmGrammarRuleDefinition integerRule() {

    var negCallParamLiteral = new AsmGrammarLiteralDefinition(
        new Identifier("INTEGER", SourceLocation.INVALID_SOURCE_LOCATION), List.of(),
        null,
        null, SourceLocation.INVALID_SOURCE_LOCATION);
    negCallParamLiteral.asmType = ConstantAsmType.instance();

    var negLiteral = new AsmGrammarLiteralDefinition(
        new Identifier(BUILTIN_ASM_NEG, SourceLocation.INVALID_SOURCE_LOCATION),
        List.of(negCallParamLiteral), null, null, SourceLocation.INVALID_SOURCE_LOCATION
    );
    negLiteral.asmType = ConstantAsmType.instance();

    var negCallElement = new AsmGrammarElementDefinition(
        null, new Identifier("val", SourceLocation.INVALID_SOURCE_LOCATION), false, negLiteral,
        null, null, null, null, null, SourceLocation.INVALID_SOURCE_LOCATION
    );
    negCallElement.asmType = ConstantAsmType.instance();

    var minusNegCallGroupElement = new AsmGrammarElementDefinition(null,
        new Identifier("dec", SourceLocation.INVALID_SOURCE_LOCATION), false, null,
        new AsmGrammarAlternativesDefinition(
            List.of(List.of(ruleReference("MINUS", StringAsmType.instance()), negCallElement)),
            SourceLocation.INVALID_SOURCE_LOCATION),
        null, null, null,
        new AsmGrammarTypeDefinition(
            new Identifier("constant", SourceLocation.INVALID_SOURCE_LOCATION),
            SourceLocation.INVALID_SOURCE_LOCATION),
        SourceLocation.INVALID_SOURCE_LOCATION
    );

    var integerAlternatives = new AsmGrammarAlternativesDefinition(
        List.of(
            List.of(ruleReference("INTEGER", ConstantAsmType.instance())),
            List.of(minusNegCallGroupElement)
        ), SourceLocation.INVALID_SOURCE_LOCATION
    );

    var rule = new AsmGrammarRuleDefinition(
        new Identifier("Integer", SourceLocation.INVALID_SOURCE_LOCATION),
        new AsmGrammarTypeDefinition(
            new Identifier(ConstantAsmType.instance().name(),
                SourceLocation.INVALID_SOURCE_LOCATION),
            SourceLocation.INVALID_SOURCE_LOCATION),
        integerAlternatives, SourceLocation.INVALID_SOURCE_LOCATION);

    return rule;
  }

  private static AsmGrammarElementDefinition instructionRule() {
    var instructionElement = ruleReference("Instruction", InstructionAsmType.instance());
    instructionElement.attribute =
        new Identifier("inst", SourceLocation.INVALID_SOURCE_LOCATION);
    return instructionElement;
  }

  private static AsmGrammarElementDefinition ruleReference(String refName) {
    return ruleReference(refName, null);
  }

  private static AsmGrammarElementDefinition ruleReference(String refName,
                                                           @Nullable AsmType refRuleType) {

    var literal = new AsmGrammarLiteralDefinition(
        new Identifier(refName, SourceLocation.INVALID_SOURCE_LOCATION),
        new ArrayList<>(), null, null, SourceLocation.INVALID_SOURCE_LOCATION
    );
    literal.asmType = refRuleType;

    var element = new AsmGrammarElementDefinition(
        null, null, false, literal,
        null, null, null, null, null, SourceLocation.INVALID_SOURCE_LOCATION
    );

    element.asmType = refRuleType;
    return element;
  }
}
