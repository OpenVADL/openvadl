package vadl.iss.passes.tcgLowering;

import static vadl.viam.ViamError.ensure;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.Nullable;
import vadl.viam.Resource;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.LetNode;

/**
 * The TcgV class represents a variable in the context of the QEMU ISS (TCG).
 * It is used to manage and generate variable names along with their widths.
 */
// TODO: Make record?
public class TcgV {

  public enum Kind {
    TMP,
    REG,
    REG_FILE
  }


  // TODO: Make private
  String name;
  Tcg_32_64 width;

  private final Kind kind;
  @Nullable
  private final Resource registerOrFile;
  @Nullable
  private final ExpressionNode regFileIndex;
  // indicates if the register is a destination register.
  // this is only useful for register file variables
  private final boolean isDest;


  private TcgV(String name, Tcg_32_64 width) {
    this.name = name;
    this.width = width;
    this.kind = Kind.TMP;
    this.isDest = false;
    this.registerOrFile = null;
    this.regFileIndex = null;
  }

  public TcgV(String name, Tcg_32_64 width, Kind kind,
              @Nullable Resource registerOrFile,
              @Nullable ExpressionNode regFileIndex,
              boolean isDest
  ) {
    this.name = name;
    this.width = width;
    this.kind = kind;
    this.registerOrFile = registerOrFile;
    this.regFileIndex = regFileIndex;
    this.isDest = isDest;
  }

  // TODO: Remove
  public static TcgV of(LetNode node, Tcg_32_64 width) {
    return new TcgV("_" + node.letName().name(), width);
  }

  // TODO: Remove
  public static TcgV of(String name, Tcg_32_64 width) {
    return new TcgV("v" + name, width);
  }

  private static final AtomicInteger counter = new AtomicInteger(0);

  // TODO: @jzottele remove this
  public static TcgV gen(Tcg_32_64 width) {
    var c = counter.getAndIncrement();
    return new TcgV("v" + c, width);
  }

  public Tcg_32_64 width() {
    return width;
  }

  public String varName() {
    return name;
  }

  public Kind kind() {
    return kind;
  }

  public Resource registerOrFile() {
    ensure(registerOrFile != null, "registerOrFile is null");
    return registerOrFile;
  }

  public ExpressionNode regFileIndex() {
    ensure(regFileIndex != null, "regFileIndex is null");
    return regFileIndex;
  }

  public boolean isDest() {
    return isDest;
  }


  // TODO: remove this
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
