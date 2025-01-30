package vadl.viam.asm.elements;

import java.util.List;
import javax.annotation.Nullable;
import vadl.viam.asm.rules.GrammarRule;

/**
 * Represents the invocation of another grammar rule in a grammar rule.
 */
public class RuleInvocation implements GrammarElement {
  @Nullable
  AssignTo assignToElement;
  GrammarRule rule;
  List<GrammarElement> parameters;

  /**
   * Creates a new RuleInvocation.
   *
   * @param assignToElement the element that stores the result of the invocation
   * @param rule            the invoked rule
   * @param parameters      the parameters of the invocation
   */
  public RuleInvocation(@Nullable AssignTo assignToElement, GrammarRule rule,
                        List<GrammarElement> parameters) {
    this.assignToElement = assignToElement;
    this.rule = rule;
    this.parameters = parameters;
  }
}
