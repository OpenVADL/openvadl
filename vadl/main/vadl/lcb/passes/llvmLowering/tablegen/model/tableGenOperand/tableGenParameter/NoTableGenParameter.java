package vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.tableGenParameter;

/**
 * Some parameters like constants have no parameter identity.
 */
public class NoTableGenParameter extends TableGenParameter {
  @Override
  public String render() {
    throw new RuntimeException(
        "This function should not be called because the operand has no identity.");
  }
}
