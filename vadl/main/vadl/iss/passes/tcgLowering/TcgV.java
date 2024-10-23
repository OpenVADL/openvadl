package vadl.iss.passes.tcgLowering;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import vadl.viam.graph.dependency.LetNode;

/**
 * The TcgV class represents a variable in the context of the QEMU ISS (TCG).
 * It is used to manage and generate variable names along with their widths.
 */
public class TcgV {

  String name;
  TcgWidth width;

  private TcgV(String name, TcgWidth width) {
    this.name = name;
    this.width = width;
  }

  public static TcgV of(LetNode node, TcgWidth width) {
    return new TcgV("_" + node.letName().name(), width);
  }

  public static TcgV of(String name, TcgWidth width) {
    return new TcgV("v" + name, width);
  }

  private static AtomicInteger counter = new AtomicInteger(0);

  // TODO: @jzottele Maybe remove this
  public static TcgV gen(TcgWidth width) {
    var c = counter.getAndIncrement();
    return new TcgV("v" + c, width);
  }

  public TcgWidth width() {
    return width;
  }

  public String varName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name + "_" + width;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    TcgV tcgV = (TcgV) o;
    return Objects.equals(name, tcgV.name);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name);
  }
}
