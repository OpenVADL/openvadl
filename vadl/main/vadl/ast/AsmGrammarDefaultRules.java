package vadl.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    return patternCache;
  }

  private static List<AsmGrammarRuleDefinition> defaultRules() {
    // regex pattern needed for checking LL(1) conflicts
    // in the AsmParser this is handled by the LLVM lexer
    return new ArrayList<>(List.of(
        terminalRuleTypeString("IDENTIFIER", "[a-zA-Z_.][a-zA-Z0-9_$.@]*", false),
        terminalRuleTypeString("STRING", "\\\".*\\\"", false),
        terminalRule("INTEGER", "0b[01]+|0[0-7]+|[1-9][0-9]*|0x[0-9a-fA-F]+", false,
            ConstantAsmType.instance()),
        terminalRuleTypeString("COLON", ":", false),
        terminalRuleTypeString("PLUS", "+", true),
        terminalRuleTypeString("MINUS", "-", false),
        terminalRuleTypeString("TILDE", "~", false),
        terminalRuleTypeString("SLASH", "/", false),
        terminalRuleTypeString("BACKSLASH", "\\\\", false),
        terminalRuleTypeString("LPAREN", "(", true),
        terminalRuleTypeString("RPAREN", ")", true),
        terminalRuleTypeString("LBRAC", "[", true),
        terminalRuleTypeString("RBRAC", "]", true),
        terminalRuleTypeString("LCURLY", "{", true),
        terminalRuleTypeString("RCURLY", "}", true),
        terminalRuleTypeString("STAR", "*", true),
        terminalRuleTypeString("DOT", ".", true),
        terminalRuleTypeString("COMMA", ",", false),
        terminalRuleTypeString("DOLLAR", "$", true),
        terminalRuleTypeString("EQUAL", "=", false),
        terminalRuleTypeString("EQUALEQUAL", "==", false),
        terminalRuleTypeString("PIPE", "|", true),
        terminalRuleTypeString("PIPEPIPE", "||", true),
        terminalRuleTypeString("CARET", "^", true),
        terminalRuleTypeString("AMP", "&", false),
        terminalRuleTypeString("AMPAMP", "&&", false),
        terminalRuleTypeString("EXCLAIM", "!", false),
        terminalRuleTypeString("EXCLAIMEQUAL", "!=", false),
        terminalRuleTypeString("PERCENT", "%", false),
        terminalRuleTypeString("HASH", "#", false),
        terminalRuleTypeString("LESS", "<", false),
        terminalRuleTypeString("LESSEQUAL", "<=", false),
        terminalRuleTypeString("LESSLESS", "<<", false),
        terminalRuleTypeString("LESSGREATER", "<>", false),
        terminalRuleTypeString("GREATER", ">", false),
        terminalRuleTypeString("GREATEREQUAL", ">=", false),
        terminalRuleTypeString("GREATERGREATER", ">>", false),
        terminalRuleTypeString("AT", "@", false),
        terminalRuleTypeString("MINUSGREATER", "->", false),
        terminalRule("EOL", "[\\r(\\r\\n)]", true, VoidAsmType.instance()),
        nonTerminalRule("Statement", InstructionAsmType.instance(), false, instructionRule(),
            ruleReference("EOL", VoidAsmType.instance())),
        nonTerminalRule("Register", RegisterAsmType.instance(),
            false, ruleReference("IDENTIFIER", StringAsmType.instance())),
        nonTerminalRule("ImmediateOperand", null, false, ruleReference("Expression")),
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
                                                                 String regularExpression,
                                                                 boolean escapeRegex) {
    return terminalRule(name, regularExpression, escapeRegex, StringAsmType.instance());
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
