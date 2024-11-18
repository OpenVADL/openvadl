package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import java.util.function.Function;
import vadl.iss.passes.tcgLowering.TcgExtend;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.iss.passes.tcgLowering.Tcg_8_16_32_64;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.graph.Node;

/**
 * Represents a TCG (Tiny Code Generation) operation node responsible for loading memory.
 *
 * <p>This class encapsulates the size, extension mode, and address of the memory to be loaded.
 * It extends the TcgOpNode class, inheriting common properties of TCG operation nodes like
 * result and width.
 */
public class TcgLoadMemory extends TcgOpNode {

  @DataValue
  Tcg_8_16_32_64 size;
  @DataValue
  TcgExtend extendMode;
  @DataValue
  TcgV addr;

  /**
   * Constructs a new TcgLoadMemory operation node.
   *
   * @param size the size of the memory to be loaded, one of Tcg_8_16_32_64
   *             values (i8, i16, i32, i64)
   * @param mode the mode of an extension, one of TcgExtend values (SIGN, ZERO)
   * @param dest the variable representing the result of the load operation
   * @param addr the variable representing the address from where memory is to be loaded
   */
  public TcgLoadMemory(Tcg_8_16_32_64 size,
                       TcgExtend mode,
                       TcgV dest,
                       TcgV addr) {
    super(dest, dest.width());
    this.size = size;
    this.extendMode = mode;
    this.addr = addr;
  }

  public Tcg_8_16_32_64 size() {
    return size;
  }

  public TcgExtend mode() {
    return extendMode;
  }

  public TcgV addr() {
    return addr;
  }

  @Override
  public String cCode(Function<Node, String> nodeToCCode) {
    return "tcg_gen_qemu_ld_" + width
        + "(" + dest().varName()
        + "," + addr().varName()
        + ", 0"
        + ", " + tcgMemOp()
        + ");";
  }

  @Override
  public Node copy() {
    return new TcgLoadMemory(size, extendMode, dest, addr);
  }

  @Override
  public Node shallowCopy() {
    return new TcgLoadMemory(size, extendMode, dest, addr);
  }

  /**
   * Generates the memory operation string for a TCG (Tiny Code Generation) load operation.
   *
   * <p>The method composes a memory operation string based on the size of the memory to be loaded
   * and the extension mode.
   * The format of the memory operation string varies depending on whether
   * the extension mode requires a sign extension or zero extension.
   *
   * @return A string representing the memory operation flags. It includes the memory operation size
   *     and, if applicable, the sign extension flag.
   */
  public String tcgMemOp() {
    var first = "MO_" + size.width;
    return switch (extendMode) {
      case SIGN -> "MO_SIGN | " + first;
      case ZERO -> first; // no second flag required
    };
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(size);
    collection.add(extendMode);
    collection.add(addr);
  }
}
