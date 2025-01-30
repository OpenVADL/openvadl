package vadl.viam.asm.elements;

import java.util.List;
import javax.annotation.Nullable;

/**
 * Represents the invocation of a vadl function invocation in a grammar rule.
 */
public class FunctionInvocation implements GrammarElement {
  @Nullable
  AssignTo assignToElement;
  // TODO: store reference to Function instead of the function name
  String functionName;
  List<GrammarElement> parameters;

  /**
   * Creates a new FunctionInvocation.
   *
   * @param assignToElement the element that stores the result of the invocation
   * @param functionName    the invoked function's name
   * @param parameters      the parameters of the invocation
   */
  public FunctionInvocation(@Nullable AssignTo assignToElement, String functionName,
                            List<GrammarElement> parameters) {
    this.assignToElement = assignToElement;
    this.functionName = functionName;
    this.parameters = parameters;
  }
}
