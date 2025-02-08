package vadl.viam.asm.elements;

import java.util.List;
import javax.annotation.Nullable;
import vadl.types.asmTypes.AsmType;
import vadl.viam.asm.AsmGrammarVisitor;
import vadl.viam.asm.rules.AsmGrammarRule;

/**
 * Represents the invocation of another grammar rule in a grammar rule.
 */
public record AsmRuleInvocation(@Nullable AsmAssignTo assignToElement,
                                AsmGrammarRule rule,
                                List<AsmGrammarElement> parameters,
                                AsmType asmType)
    implements AsmGrammarElement {

  @Override
  public void accept(AsmGrammarVisitor visitor) {
    visitor.visit(this);
  }
}
