package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import vadl.iss.passes.tcgLowering.TcgExtend;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.iss.passes.tcgLowering.TcgWidth;
import vadl.iss.passes.tcgLowering.Tcg_8_16_32_64;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.graph.Node;

public class TcgLoadMemory extends TcgOpNode {

  @DataValue
  Tcg_8_16_32_64 size;
  @DataValue
  TcgExtend extendMode;
  @DataValue
  TcgV addr;

  public TcgLoadMemory(Tcg_8_16_32_64 size,
                       TcgExtend mode,
                       TcgV res,
                       TcgV addr,
                       TcgWidth width) {
    super(res, width);
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
  public Node copy() {
    return new TcgLoadMemory(size, extendMode, res, addr, width);
  }

  @Override
  public Node shallowCopy() {
    return new TcgLoadMemory(size, extendMode, res, addr, width);
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
