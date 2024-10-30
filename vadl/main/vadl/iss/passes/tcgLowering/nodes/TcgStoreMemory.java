package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import vadl.iss.passes.tcgLowering.TcgExtend;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.iss.passes.tcgLowering.TcgWidth;
import vadl.iss.passes.tcgLowering.Tcg_8_16_32_64;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.graph.Node;

/**
 * Represents a memory store operation in the Tiny Code Generation (TCG) framework.
 * This class is a specific node type that encapsulates storing a value into memory.
 */
public class TcgStoreMemory extends TcgOpNode {

  @DataValue
  Tcg_8_16_32_64 size;
  @DataValue
  TcgExtend extendMode;
  @DataValue
  TcgV addr;

  /**
   * Constructs a TcgStoreMemory operation node which is used to store a value into memory
   * within the Tiny Code Generation (TCG) framework.
   *
   * @param size The size of the memory to write, represented by `Tcg_8_16_32_64`.
   * @param mode The extension mode for the value, represented by `TcgExtend`.
   * @param val  The value to be stored into memory, represented by `TcgV`.
   * @param addr The address in memory where the value is to be stored, represented by `TcgV`.
   */
  public TcgStoreMemory(Tcg_8_16_32_64 size,
                        TcgExtend mode,
                        TcgV val,
                        TcgV addr) {
    super(val, val.width());
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

  public TcgV val() {
    return res();
  }

  @Override
  public Node copy() {
    return new TcgStoreMemory(size, extendMode, res, addr);
  }

  @Override
  public Node shallowCopy() {
    return new TcgStoreMemory(size, extendMode, res, addr);
  }

  /**
   * Generates a memory operation string based on the size and extension mode.
   *
   * @return A string representing the memory operation with the appropriate
   *       size and extension flag.
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
