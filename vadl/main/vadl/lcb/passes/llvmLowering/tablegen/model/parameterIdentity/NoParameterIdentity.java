package vadl.lcb.passes.llvmLowering.tablegen.model.parameterIdentity;

/**
 * Some parameters like constants have no parameter identity.
 */
public class NoParameterIdentity extends ParameterIdentity {
  @Override
  public String render() {
    throw new RuntimeException(
        "This function should not be called because the operand has no identity.");
  }
}
