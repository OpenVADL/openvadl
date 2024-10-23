package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.iss.passes.tcgLowering.TcgWidth;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.graph.Node;

public class TcgLoadMemory extends TcgOpNode {

  public enum LoadSize {
    BYTE_8,
    WORD_16,
    DWORD_32,
    QWORD_64;

    public static LoadSize fromWidth(int width) {
      return switch (width) {
        case 8 -> BYTE_8;
        case 16 -> WORD_16;
        case 32 -> DWORD_32;
        case 64 -> QWORD_64;
        default -> throw new IllegalArgumentException("Invalid width: " + width);
      };
    }
  }

  public enum ExtendMode {
    ZERO_EXTEND,
    SIGN_EXTEND;
  }

  @DataValue
  LoadSize size;
  @DataValue
  ExtendMode mode;
  @DataValue
  TcgV addr;

  public TcgLoadMemory(LoadSize size,
                       ExtendMode mode,
                       TcgV res,
                       TcgV addr,
                       TcgWidth width) {
    super(res, width);
    this.size = size;
    this.mode = mode;
    this.addr = addr;
  }

  public LoadSize size() {
    return size;
  }

  public ExtendMode mode() {
    return mode;
  }

  @Override
  public Node copy() {
    return new TcgLoadMemory(size, mode, res, addr, width);
  }

  @Override
  public Node shallowCopy() {
    return new TcgLoadMemory(size, mode, res, addr, width);
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(size);
    collection.add(mode);
    collection.add(addr);
  }
}
