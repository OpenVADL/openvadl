package vadl.lcb.template.utils;

/**
 * LLVM requires the data layout on multiple places in the code. This class
 * unifies access for it in vadl.
 */
public class DataLayoutProvider {

  // TODO(kper): remove this when abi is defined
  public static DataLayout RISCV_DATALAYOUT = new DataLayout(false, 32, 32);

  /**
   * Holds information about the data layout of the target.
   */
  public record DataLayout(boolean isBigEndian, int pointerSize, int pointerAlignment) {
  }

  /**
   * Creates a string representation from {@link DataLayout}.
   */
  public static String createDataLayout(DataLayout dataLayout) {
    String loweredEndian = dataLayout.isBigEndian ? "E-" : "e-";
    String loweredPointer =
        String.format("p:%d:%d-", dataLayout.pointerSize, dataLayout.pointerSize);
    return String.format("%sm:e-%sS0-a:0:64-i1:8:8-i8:8:8-i16:16:16-i32:32:32-i64:32:64",
        loweredEndian, loweredPointer);
  }
}
