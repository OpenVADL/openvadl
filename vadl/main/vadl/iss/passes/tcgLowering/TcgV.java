package vadl.iss.passes.tcgLowering;

import static vadl.viam.ViamError.ensure;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import vadl.viam.Register;
import vadl.viam.RegisterFile;
import vadl.viam.Resource;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.LetNode;

/**
 * The TcgV class represents a variable in the context of the QEMU ISS (TCG).
 * It is used to manage and generate variable names along with their widths.
 */
public class TcgV {

  /**
   * The kind of TCGv. This can be a temporary, constant, register or register file.
   */
  public enum Kind {
    TMP,
    CONST,
    REG,
    REG_FILE
  }

  private final String name;
  private final Tcg_32_64 width;

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

  /**
   * Constructs the TcgV.
   *
   * @param name           of the variable
   * @param width          of the variable
   * @param kind           of the variable
   * @param constValue     expression of the variable (must be set if kind is CONST)
   * @param registerOrFile register or register file represented by the variable
   *                       (must be set if kind is REG or REG_FILE)
   * @param regFileIndex   index of register file (must be set if kind is REG_FILE)
   * @param isDest         must be true if the variable is used as write location of a side-effect.
   */
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
