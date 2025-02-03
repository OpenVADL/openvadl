package vadl.viam.asm.elements;

import java.util.List;
import javax.annotation.Nullable;

/**
 * Represents the invocation of a vadl function invocation in a grammar rule.
 */
public class AsmFunctionInvocation implements AsmGrammarElement {
  @Nullable
  AsmAssignTo assignToElement;
  // TODO: store reference to Function instead of the function name
  String functionName;
  List<AsmGrammarElement> parameters;

  /**
   * Creates a new FunctionInvocation.
   *
   * @param assignToElement the element that stores the result of the invocation
   * @param functionName    the invoked function's name
   * @param parameters      the parameters of the invocation
   */
  public AsmFunctionInvocation(@Nullable AsmAssignTo assignToElement, String functionName,
                               List<AsmGrammarElement> parameters) {
    this.assignToElement = assignToElement;
    this.functionName = functionName;
    this.parameters = parameters;
  }
}
