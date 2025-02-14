package vadl.viam.asm.elements;

import java.util.List;
import javax.annotation.Nullable;
import vadl.types.asmTypes.AsmType;
import vadl.viam.Function;
import vadl.viam.asm.AsmGrammarVisitor;

/**
 * Represents the invocation of a vadl function invocation in a grammar rule.
 */
public record AsmFunctionInvocation(@Nullable AsmAssignTo assignToElement,
                                    Function function,
                                    List<AsmGrammarElement> parameters,
                                    AsmType asmType)
    implements AsmGrammarElement {

  @Override
  public void accept(AsmGrammarVisitor visitor) {
    visitor.visit(this);
  }
}
