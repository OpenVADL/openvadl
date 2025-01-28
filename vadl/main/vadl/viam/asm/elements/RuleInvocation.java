package vadl.viam.asm.elements;

import java.util.List;
import javax.annotation.Nullable;

/**
 * Represents the invocation of another grammar rule in a grammar rule.
 */
public class RuleInvocation implements GrammarElement {
  @Nullable
  String attributeOrLocalVar;
  String ruleName;
  List<GrammarElement> parameters;

  public RuleInvocation(@Nullable String attributeOrLocalVar, String ruleName,
                        List<GrammarElement> parameters) {
    this.attributeOrLocalVar = attributeOrLocalVar;
    this.ruleName = ruleName;
    this.parameters = parameters;
  }
}
