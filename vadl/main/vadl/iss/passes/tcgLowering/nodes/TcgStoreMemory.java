package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import vadl.iss.passes.tcgLowering.TcgExtend;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.iss.passes.tcgLowering.TcgWidth;
import vadl.iss.passes.tcgLowering.Tcg_8_16_32_64;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.graph.Node;

public class TcgStoreMemory extends TcgOpNode {

  @DataValue
  Tcg_8_16_32_64 size;
  @DataValue
  TcgExtend extendMode;
  @DataValue
  TcgV addr;

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

  public String tcgMemOp() {
    var first = "MO_" + size.width;
    return switch (extendMode) {
        case SIGN -> "MO_SIGN | " + first ;
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
