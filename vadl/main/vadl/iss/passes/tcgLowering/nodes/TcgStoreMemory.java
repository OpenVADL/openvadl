package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nullable;
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.iss.passes.tcgLowering.TcgExtend;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.iss.passes.tcgLowering.Tcg_8_16_32_64;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;

/**
 * Represents a memory store operation in the Tiny Code Generation (TCG) framework.
 * This class is a specific node type that encapsulates storing a value into memory.
 */
public class TcgStoreMemory extends TcgNode {

  @DataValue
  Tcg_8_16_32_64 size;
  @DataValue
  TcgExtend extendMode;
  @Input
  TcgVRefNode addr;
  @Input
  TcgVRefNode val;

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
                        TcgVRefNode val,
                        TcgVRefNode addr) {
    this.size = size;
    this.extendMode = mode;
    this.addr = addr;
    this.val = val;
  }

  public Tcg_8_16_32_64 size() {
    return size;
  }

  public TcgExtend mode() {
    return extendMode;
  }

  public TcgVRefNode addr() {
    return addr;
  }

  public TcgVRefNode val() {
    return val;
  }

  @Override
  public String cCode(Function<Node, String> nodeToCCode) {
    return "tcg_gen_qemu_st_" + val.width()
        + "(" + val().varName()
        + "," + addr().varName()
        + ", 0"
        + ", " + tcgMemOp()
        + ");";
  }

  @Override
  public Set<TcgVRefNode> usedVars() {
    return Set.of(addr, val);
  }

  @Override
  public @Nullable TcgVRefNode definedVar() {
    return null;
  }


  @Override
  public Node copy() {
    return new TcgStoreMemory(size, extendMode, val, addr);
  }

  @Override
  public Node shallowCopy() {
    return new TcgStoreMemory(size, extendMode, val, addr);
  }

  /**
   * Generates a memory operation string based on the size and extension mode.
   *
   * @return A string representing the memory operation with the appropriate
   *     size and extension flag.
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
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(addr);
    collection.add(val);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    addr = visitor.apply(this, addr, TcgVRefNode.class);
    val = visitor.apply(this, val, TcgVRefNode.class);
  }
}
