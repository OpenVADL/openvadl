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

package vadl.viam;

import javax.annotation.Nullable;
import vadl.types.ConcreteRelationType;
import vadl.types.DataType;
import vadl.types.Type;

/**
 * Represents a Register in a VADL specification.
 *
 * <p>It might have sub registers and it might be a sub register with a parent. Additionally,
 * registers might have a reference format, used to access sub fields by slicing.
 * If partial (sub register) or full (slicing) access is used, depends on the {@link AccessKind}.
 * </p>
 */
public class Register extends Resource {

  /**
   * Defines if a sub-register is accessed by loading the whole register and slicing the result,
   * or by directly accessing the partial result. Same for writing a register.
   */
  public enum AccessKind {
    PARTIAL,
    FULL
  }

  private final DataType resultType;

  @Nullable
  private Register parent;
  private final Register[] subRegisters;

  private final AccessKind readAccess;
  private final AccessKind writeAccess;

  @Nullable
  private final Format refFormat;

  /**
   * Constructions a new register definition.
   *
   * @param identifier   the unique identifier of the definition
   * @param resultType   the result type of the register
   * @param readAccess   the read access of the register (see {@link AccessKind})
   * @param writeAccess  the write access of the register (see {@link AccessKind})
   * @param refFormat    the register's format, if it was used as type in the VADL specification
   * @param subRegisters the sub-registers of the register
   */
  public Register(Identifier identifier, DataType resultType, AccessKind readAccess,
                  AccessKind writeAccess, @Nullable Format refFormat, Register[] subRegisters) {
    super(identifier);
    this.resultType = resultType;
    this.subRegisters = subRegisters;
    this.readAccess = readAccess;
    this.writeAccess = writeAccess;
    this.refFormat = refFormat;
  }

  @Override
  public boolean hasAddress() {
    return false;
  }

  @Nullable
  @Override
  public DataType addressType() {
    return null;
  }

  @Override
  public DataType resultType() {
    return resultType;
  }

  @Override
  public ConcreteRelationType relationType() {
    return Type.concreteRelation(resultType);
  }

  public boolean isSubRegister() {
    return this.parent != null;
  }

  public AccessKind readAccess() {
    return readAccess;
  }

  public AccessKind writeAccess() {
    return writeAccess;
  }

  public @Nullable Register parent() {
    return this.parent;
  }

  @SuppressWarnings("NullableProblems")
  public void setParent(Register parent) {
    this.parent = parent;
  }

  public Register[] subRegisters() {
    return subRegisters;
  }

  public @Nullable Format refFormat() {
    return refFormat;
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    return identifier.simpleName() + ": " + resultType;
  }

}
