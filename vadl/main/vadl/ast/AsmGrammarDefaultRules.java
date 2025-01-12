package vadl.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import vadl.types.asmTypes.AsmType;
import vadl.types.asmTypes.ConstantAsmType;
import vadl.types.asmTypes.ExpressionAsmType;
import vadl.types.asmTypes.InstructionAsmType;
import vadl.types.asmTypes.RegisterAsmType;
import vadl.types.asmTypes.StringAsmType;
import vadl.types.asmTypes.SymbolAsmType;
import vadl.types.asmTypes.VoidAsmType;
import vadl.utils.SourceLocation;

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
  public static final String BUILTIN_ASM_NEG = "builtin_asm_neg";

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
    if (patternCache.entrySet().isEmpty()) {
      defaultRules().stream()
          .filter(rule -> rule.isTerminalRule)
          .forEach(rule -> {
            var literal = rule.alternatives.alternatives.get(0).get(0).asmLiteral;
            Objects.requireNonNull(literal);
            Objects.requireNonNull(literal.stringLiteral);
            var regex = ((StringLiteral) literal.stringLiteral).value;
            patternCache.put(rule, Pattern.compile(regex));
          });
    }
    return patternCache;
  }

  private static List<AsmGrammarRuleDefinition> defaultRules() {
    // regex pattern needed for checking LL(1) conflicts
    // in the AsmParser this is handled by the LLVM lexer
    return new ArrayList<>(List.of(
        terminalRuleTypeString("IDENTIFIER", "[a-zA-Z_.][a-zA-Z0-9_$.@]*"),
        terminalRuleTypeString("STRING", "\\\".*\\\""),
        terminalRuleTypeString("INTEGER", "0b[01]+|0[0-7]+|[1-9][0-9]*|0x[0-9a-fA-F]+"),
        terminalRuleTypeString("COLON", ":"),
        terminalRuleTypeString("PLUS", "\\+"),
        terminalRuleTypeString("MINUS", "-"),
        terminalRuleTypeString("TILDE", "~"),
        terminalRuleTypeString("SLASH", "/"),
        terminalRuleTypeString("BACKSLASH", "\\\\"),
        terminalRuleTypeString("LPAREN", "\\("),
        terminalRuleTypeString("RPAREN", "\\)"),
        terminalRuleTypeString("LBRAC", "\\["),
        terminalRuleTypeString("RBRAC", "\\]"),
        terminalRuleTypeString("LCURLY", "\\{"),
        terminalRuleTypeString("RCURLY", "\\}"),
        terminalRuleTypeString("STAR", "\\*"),
        terminalRuleTypeString("DOT", "\\."),
        terminalRuleTypeString("COMMA", ","),
        terminalRuleTypeString("DOLLAR", "$"),
        terminalRuleTypeString("EQUAL", "="),
        terminalRuleTypeString("EQUALEQUAL", "=="),
        terminalRuleTypeString("PIPE", "|"),
        terminalRuleTypeString("PIPEPIPE", "||"),
        terminalRuleTypeString("CARET", "^"),
        terminalRuleTypeString("AMP", "&"),
        terminalRuleTypeString("AMPAMP", "&&"),
        terminalRuleTypeString("EXCLAIM", "!"),
        terminalRuleTypeString("EXCLAIMEQUAL", "!="),
        terminalRuleTypeString("PERCENT", "%"),
        terminalRuleTypeString("HASH", "#"),
        terminalRuleTypeString("LESS", "<"),
        terminalRuleTypeString("LESSEQUAL", "<="),
        terminalRuleTypeString("LESSLESS", "<<"),
        terminalRuleTypeString("LESSGREATER", "<>"),
        terminalRuleTypeString("GREATER", ">"),
        terminalRuleTypeString("GREATEREQUAL", ">="),
        terminalRuleTypeString("GREATERGREATER", ">>"),
        terminalRuleTypeString("AT", "@"),
        terminalRuleTypeString("MINUSGREATER", "->"),
        terminalRule("EOL", "[\\r(\\r\\n)]", VoidAsmType.instance()),
        nonTerminalRule("Statement", InstructionAsmType.instance(), false, instructionRule(),
            ruleReference("EOL")),
        nonTerminalRule("Register", RegisterAsmType.instance(),
            false, ruleReference("IDENTIFIER")),
        nonTerminalRule("ImmediateOperand", null, false, ruleReference("Expression")),
        nonTerminalRule("Identifier", StringAsmType.instance(),
            false, ruleReference("IDENTIFIER")),
        nonTerminalRule("Expression", ExpressionAsmType.instance(), true,
            ruleReference("Expression")),
        nonTerminalRule("Instruction", InstructionAsmType.instance(), true,
            ruleReference("Instruction")),
        integerRule(),
        nonTerminalRule("Natural", ConstantAsmType.instance(), false, ruleReference("INTEGER")),
        nonTerminalRule("Label", SymbolAsmType.instance(), false, ruleReference("Identifier"))
    ));
  }

  /**
   * Defines a function that negates a 64-bit integer.
   * This function is used in the default grammar rules to be able to parse negative constants.
   *
   * @return a function definition that negates a 64-bit integer
   */
  public static FunctionDefinition asmNegFunctionDefinition() {
    return new FunctionDefinition(
        new Identifier(BUILTIN_ASM_NEG, SourceLocation.INVALID_SOURCE_LOCATION),
        new ArrayList<>(List.of(
            new Parameter(
                new Identifier("x", SourceLocation.INVALID_SOURCE_LOCATION),
                new TypeLiteral(new Identifier("SInt", SourceLocation.INVALID_SOURCE_LOCATION),
                    List.of(
                        List.of(new IntegerLiteral("64", SourceLocation.INVALID_SOURCE_LOCATION))),
                    SourceLocation.INVALID_SOURCE_LOCATION)
            )
        )),
        new TypeLiteral(new Identifier("SInt", SourceLocation.INVALID_SOURCE_LOCATION),
            List.of(
                List.of(new IntegerLiteral("64", SourceLocation.INVALID_SOURCE_LOCATION))),
            SourceLocation.INVALID_SOURCE_LOCATION),
        new UnaryExpr(new UnOp(UnaryOperator.NEGATIVE, SourceLocation.INVALID_SOURCE_LOCATION),
            new Identifier("x", SourceLocation.INVALID_SOURCE_LOCATION)),
        SourceLocation.INVALID_SOURCE_LOCATION
    );
  }

  private static AsmGrammarRuleDefinition terminalRuleTypeString(String name,
                                                                 String regularExpression) {
    return terminalRule(name, regularExpression, StringAsmType.instance());
  }

  private static AsmGrammarRuleDefinition terminalRule(String name, String regularExpression,
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
    return rule;
  }

  private static AsmGrammarRuleDefinition nonTerminalRule(String name, @Nullable AsmType ruleType,
                                                          boolean isBuiltinRule,
                                                          AsmGrammarElementDefinition... elements) {
    var rule = new AsmGrammarRuleDefinition(
        new Identifier(name, SourceLocation.INVALID_SOURCE_LOCATION),
        null,
        new AsmGrammarAlternativesDefinition(
            new ArrayList<>(List.of(List.of(elements))),
            SourceLocation.INVALID_SOURCE_LOCATION
        ),
        SourceLocation.INVALID_SOURCE_LOCATION
    );

    // by setting the asmType we can avoid checking the default rules in the typechecker
    rule.asmType = ruleType;
    rule.isBuiltinRule = isBuiltinRule;
    return rule;
  }

  private static AsmGrammarRuleDefinition integerRule() {

    var negCallElement = new AsmGrammarElementDefinition(
        null, null, false,
        new AsmGrammarLiteralDefinition(
            new Identifier(BUILTIN_ASM_NEG, SourceLocation.INVALID_SOURCE_LOCATION),
            List.of(
                new AsmGrammarLiteralDefinition(
                    new Identifier("INTEGER", SourceLocation.INVALID_SOURCE_LOCATION), List.of(),
                    null,
                    null, SourceLocation.INVALID_SOURCE_LOCATION)
            ), null, null, SourceLocation.INVALID_SOURCE_LOCATION
        ),
        null, null, null, null, null, SourceLocation.INVALID_SOURCE_LOCATION
    );

    var rule = new AsmGrammarRuleDefinition(
        new Identifier("Integer", SourceLocation.INVALID_SOURCE_LOCATION),
        new AsmGrammarTypeDefinition(
            new Identifier(ConstantAsmType.instance().name(),
                SourceLocation.INVALID_SOURCE_LOCATION),
            SourceLocation.INVALID_SOURCE_LOCATION),
        new AsmGrammarAlternativesDefinition(
            List.of(
                List.of(ruleReference("MINUS"), negCallElement),
                List.of(ruleReference("INTEGER", ConstantAsmType.instance()))
            ), SourceLocation.INVALID_SOURCE_LOCATION
        ), SourceLocation.INVALID_SOURCE_LOCATION);

    // by setting the asmType we can avoid checking the default rules in the typechecker
    rule.asmType = ConstantAsmType.instance();
    return rule;
  }

  private static AsmGrammarElementDefinition instructionRule() {
    var instructionElement = ruleReference("Instruction", InstructionAsmType.instance());
    instructionElement.attribute =
        new Identifier("instruction", SourceLocation.INVALID_SOURCE_LOCATION);
    return instructionElement;
  }

  private static AsmGrammarElementDefinition ruleReference(String refName) {
    return ruleReference(refName, null);
  }

  private static AsmGrammarElementDefinition ruleReference(String refName,
                                                           @Nullable AsmType refRuleType) {
    var element = new AsmGrammarElementDefinition(
        null, null, false,
        new AsmGrammarLiteralDefinition(
            new Identifier(refName, SourceLocation.INVALID_SOURCE_LOCATION),
            new ArrayList<>(), null, null, SourceLocation.INVALID_SOURCE_LOCATION
        ),
        null, null, null, null, null, SourceLocation.INVALID_SOURCE_LOCATION
    );

    element.asmType = refRuleType;
    return element;
  }
}
