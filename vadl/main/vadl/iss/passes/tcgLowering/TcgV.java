package vadl.iss.passes.tcgLowering;

import static vadl.viam.ViamError.ensure;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import org.checkerframework.checker.units.qual.N;
import org.jetbrains.annotations.Nullable;
import vadl.viam.Register;
import vadl.viam.RegisterFile;
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
    CONST,
    REG,
    REG_FILE
  }


  // TODO: Make private
  String name;
  Tcg_32_64 width;

  private final Kind kind;
  @Nullable
  private final ExpressionNode constValue;
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
    this.constValue = null;
  }

  public TcgV(String name, Tcg_32_64 width, Kind kind,
              @Nullable ExpressionNode constValue,
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
    this.constValue = constValue;
  }

  // TODO: Remove
  public static TcgV of(LetNode node, Tcg_32_64 width) {
    return new TcgV("_" + node.letName().name(), width);
  }

  // TODO: Remove
  public static TcgV of(String name, Tcg_32_64 width) {
    return new TcgV("v" + name, width);
  }

  public static TcgV tmp(String name, Tcg_32_64 width) {
    return new TcgV(name, width, Kind.TMP, null, null, null, false);
  }

  public static TcgV constant(String name, Tcg_32_64 width, ExpressionNode constValue) {
    return new TcgV(name, width, Kind.CONST, constValue, null, null, false);
  }

  public static TcgV reg(String name, Tcg_32_64 width, Register reg) {
    return new TcgV(name, width, Kind.REG, null, reg, null, false);
  }

  public static TcgV regFile(String name, Tcg_32_64 width, RegisterFile regFile,
                             ExpressionNode regFileIndex, boolean isDest) {
    return new TcgV(name, width, Kind.REG_FILE, null, regFile, regFileIndex, isDest);
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

  public ExpressionNode constValue() {
    ensure(constValue != null, "constValue is null");
    return constValue;
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
