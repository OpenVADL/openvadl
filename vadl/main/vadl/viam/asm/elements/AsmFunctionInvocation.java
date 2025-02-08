package vadl.viam.asm.elements;

import java.util.List;
import javax.annotation.Nullable;
import vadl.types.asmTypes.AsmType;
import vadl.viam.asm.AsmGrammarVisitor;

/**
 * Represents the invocation of a vadl function invocation in a grammar rule.
 *
 * @param functionName TODO: store reference to Function instead of the function name
 */
public record AsmFunctionInvocation(@Nullable AsmAssignTo assignToElement,
                                    String functionName,
                                    List<AsmGrammarElement> parameters,
                                    AsmType asmType)
    implements AsmGrammarElement {

  @Override
  public void accept(AsmGrammarVisitor visitor) {
    visitor.visit(this);
  }
}
