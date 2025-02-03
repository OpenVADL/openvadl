package vadl.viam.asm.elements;

import java.util.List;
import javax.annotation.Nullable;
import vadl.viam.asm.rules.AsmGrammarRule;

/**
 * Represents the invocation of another grammar rule in a grammar rule.
 */
public class AsmRuleInvocation implements AsmGrammarElement {
  @Nullable
  AsmAssignTo assignToElement;
  AsmGrammarRule rule;
  List<AsmGrammarElement> parameters;

  /**
   * Creates a new RuleInvocation.
   *
   * @param assignToElement the element that stores the result of the invocation
   * @param rule            the invoked rule
   * @param parameters      the parameters of the invocation
   */
  public AsmRuleInvocation(@Nullable AsmAssignTo assignToElement, AsmGrammarRule rule,
                           List<AsmGrammarElement> parameters) {
    this.assignToElement = assignToElement;
    this.rule = rule;
    this.parameters = parameters;
  }
}
