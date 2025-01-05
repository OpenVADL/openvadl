package vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.tableGenParameter;

/**
 * The idea of a parameter identity is that operands in the selection and machine pattern
 * can be both matched and replaced. This can be useful to change operands like {@code AddrFI}.
 */
public abstract class TableGenParameter {
  public static final String AS_LABEL = "AsLabel";

  /**
   * Render the parameter identity to a string.
   */
  public abstract String render();
}
