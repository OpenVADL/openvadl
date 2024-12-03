package vadl.ast;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import vadl.types.AsmType;
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

  private static List<AsmGrammarRuleDefinition> defaultRules() {
    // regex pattern needed for checking LL(1) conflicts
    // in the AsmParser this is handled by the LLVM lexer
    return new ArrayList<>(List.of(
        terminalRule("IDENTIFIER", "[a-zA-Z_.][a-zA-Z0-9_$.@]*"),
        terminalRule("STRING", "\\\".*\\\""),
        terminalRule("INTEGER", "0b[01]+|0[0-7]+|[1-9][0-9]*|0x[0-9a-fA-F]+"),
        terminalRule("COLON", ":"),
        terminalRule("PLUS", "+"),
        terminalRule("MINUS", "-"),
        terminalRule("TILDE", "~"),
        terminalRule("SLASH", "/"),
        terminalRule("BACKSLASH", "\\\\"),
        terminalRule("LPAREN", "("),
        terminalRule("RPAREN", ")"),
        terminalRule("LBRAC", "["),
        terminalRule("RBRAC", "]"),
        terminalRule("LCURLY", "{"),
        terminalRule("RCURLY", "}"),
        terminalRule("STAR", "*"),
        terminalRule("DOT", "."),
        terminalRule("COMMA", ","),
        terminalRule("DOLLAR", "$"),
        terminalRule("EQUAL", "="),
        terminalRule("EQUALEQUAL", "=="),
        terminalRule("PIPE", "|"),
        terminalRule("PIPEPIPE", "||"),
        terminalRule("CARET", "^"),
        terminalRule("AMP", "&"),
        terminalRule("AMPAMP", "&&"),
        terminalRule("EXCLAIM", "!"),
        terminalRule("EXCLAIMEQUAL", "!="),
        terminalRule("PERCENT", "%"),
        terminalRule("HASH", "#"),
        terminalRule("LESS", "<"),
        terminalRule("LESSEQUAL", "<="),
        terminalRule("LESSLESS", "<<"),
        terminalRule("LESSGREATER", "<>"),
        terminalRule("GREATER", ">"),
        terminalRule("GREATEREQUAL", ">="),
        terminalRule("GREATERGREATER", ">>"),
        terminalRule("AT", "@"),
        terminalRule("MINUSGREATER", "->"),
        terminalRule("EOL", "[\\r(\\r\\n)]"),
        nonTerminalRule("Statement", instructionRule(), ruleReference("EOL")),
        nonTerminalRule("Register", ruleReference("IDENTIFIER", AsmType.REGISTER)),
        nonTerminalRule("ImmediateOperand", ruleReference("Expression")),
        nonTerminalRule("Identifier", ruleReference("IDENTIFIER")),
        nonTerminalRule("Expression", ruleReference("Expression", AsmType.EXPRESSION)),
        nonTerminalRule("Instruction", ruleReference("Instruction", AsmType.INSTRUCTION)),
        integerRule(),
        nonTerminalRule("Natural", ruleReference("INTEGER")),
        nonTerminalRule("Label", ruleReference("Identifier", AsmType.SYMBOL))
    ));
  }

  private static AsmGrammarRuleDefinition terminalRule(String name, String regularExpression) {
    return new AsmGrammarRuleDefinition(
        new Identifier(name, SourceLocation.INVALID_SOURCE_LOCATION),
        null,
        new AsmGrammarAlternativesDefinition(
            new ArrayList<>(List.of(List.of(
                new AsmGrammarElementDefinition(
                    null, null, false,
                    new AsmGrammarLiteralDefinition(
                        null, new ArrayList<>(), new StringLiteral(regularExpression), null,
                        SourceLocation.INVALID_SOURCE_LOCATION
                    ), null, null, null, null, null, SourceLocation.INVALID_SOURCE_LOCATION
                )
            ))),
            SourceLocation.INVALID_SOURCE_LOCATION
        ),
        SourceLocation.INVALID_SOURCE_LOCATION
    );
  }

  private static AsmGrammarRuleDefinition nonTerminalRule(String name,
                                                          AsmGrammarElementDefinition... elements) {
    return new AsmGrammarRuleDefinition(
        new Identifier(name, SourceLocation.INVALID_SOURCE_LOCATION),
        null,
        new AsmGrammarAlternativesDefinition(
            new ArrayList<>(List.of(List.of(elements))),
            SourceLocation.INVALID_SOURCE_LOCATION
        ),
        SourceLocation.INVALID_SOURCE_LOCATION
    );
  }

  private static AsmGrammarRuleDefinition integerRule() {
    return new AsmGrammarRuleDefinition(
        new Identifier("Integer", SourceLocation.INVALID_SOURCE_LOCATION),
        new AsmGrammarTypeDefinition(
            new Identifier(AsmType.CONSTANT.toString().toLowerCase(),
                SourceLocation.INVALID_SOURCE_LOCATION),
            SourceLocation.INVALID_SOURCE_LOCATION),
        new AsmGrammarAlternativesDefinition(
            List.of(
                //List.of(), // TODO add alternative for negative integers
                List.of(ruleReference("INTEGER", AsmType.CONSTANT))
            ), SourceLocation.INVALID_SOURCE_LOCATION
        ), SourceLocation.INVALID_SOURCE_LOCATION);
  }

  private static AsmGrammarElementDefinition instructionRule() {
    var instructionElement = ruleReference("Instruction", AsmType.INSTRUCTION);
    instructionElement.attribute =
        new Identifier("instruction", SourceLocation.INVALID_SOURCE_LOCATION);
    return instructionElement;
  }

  private static AsmGrammarElementDefinition ruleReference(String refName) {
    return ruleReference(refName, null);
  }

  private static AsmGrammarElementDefinition ruleReference(String refName,
                                                           @Nullable AsmType refRuleType) {
    AsmGrammarTypeDefinition asmTypeDef = null;
    if (refRuleType != null) {
      asmTypeDef = new AsmGrammarTypeDefinition(
          new Identifier(refRuleType.toString().toLowerCase(),
              SourceLocation.INVALID_SOURCE_LOCATION),
          SourceLocation.INVALID_SOURCE_LOCATION
      );
    }
    return new AsmGrammarElementDefinition(
        null, null, false,
        new AsmGrammarLiteralDefinition(
            new Identifier(refName, SourceLocation.INVALID_SOURCE_LOCATION),
            new ArrayList<>(), null, asmTypeDef, SourceLocation.INVALID_SOURCE_LOCATION
        ),
        null, null, null, null, null, SourceLocation.INVALID_SOURCE_LOCATION
    );
  }
}
