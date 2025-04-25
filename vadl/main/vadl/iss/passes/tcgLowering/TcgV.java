// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.iss.passes.tcgLowering;

import static vadl.viam.ViamError.ensure;

import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.viam.RegisterTensor;
import vadl.viam.graph.dependency.ExpressionNode;

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
    REG_TENSOR
  }

  private final String name;
  private final Tcg_32_64 width;

  private final Kind kind;
  @Nullable
  private final ExpressionNode constValue;
  @Nullable
  private final RegisterTensor registerOrFile;
  @Nullable
  private final List<ExpressionNode> regIndices;
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
   * @param regIndices     index of register file (must be set if kind is REG_FILE)
   * @param isDest         must be true if the variable is used as write location of a side-effect.
   */
  public TcgV(String name, Tcg_32_64 width, Kind kind,
              @Nullable ExpressionNode constValue,
              @Nullable RegisterTensor registerOrFile,
              @Nullable List<ExpressionNode> regIndices,
              boolean isDest
  ) {
    this.name = name;
    this.width = width;
    this.kind = kind;
    this.registerOrFile = registerOrFile;
    this.regIndices = regIndices;
    this.isDest = isDest;
    this.constValue = constValue;
  }

  public static TcgV tmp(String name, Tcg_32_64 width) {
    return new TcgV(name, width, Kind.TMP, null, null, null, false);
  }

  public static TcgV constant(String name, Tcg_32_64 width, ExpressionNode constValue) {
    return new TcgV(name, width, Kind.CONST, constValue, null, null, false);
  }

  public static TcgV reg(String name, Tcg_32_64 width, RegisterTensor regFile,
                         List<ExpressionNode> indices, boolean isDest) {
    return new TcgV(name, width, Kind.REG_TENSOR, null, regFile, indices, isDest);
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

  public RegisterTensor registerOrFile() {
    ensure(registerOrFile != null, "registerOrFile is null");
    return registerOrFile;
  }

  public List<ExpressionNode> regIndices() {
    ensure(regIndices != null, "regFileIndex is null");
    return regIndices;
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
