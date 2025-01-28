package vadl.viam.asm.elements;

import java.util.List;
import javax.annotation.Nullable;

/**
 * Represents the invocation of a vadl function invocation in a grammar rule.
 */
public class FunctionInvocation implements GrammarElement {
  @Nullable
  String attributeOrLocalVar;
  String functionName;
  List<GrammarElement> parameters;

  public FunctionInvocation(@Nullable String attributeOrLocalVar, String functionName,
                            List<GrammarElement> parameters) {
    this.attributeOrLocalVar = attributeOrLocalVar;
    this.functionName = functionName;
    this.parameters = parameters;
  }
}
