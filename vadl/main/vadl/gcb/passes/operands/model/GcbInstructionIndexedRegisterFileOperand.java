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

package vadl.gcb.passes.operands.model;

import java.util.Objects;
import vadl.viam.Format;
import vadl.viam.RegisterTensor;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.ReadArtificialResNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.WriteArtificialResNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;

/**
 * Indicates that the operand is a registerFile when the address is *not*
 * a {@link Format.Field} but is indexed by a function. This is useful when we have to generate
 * tablegen instruction operands from operands.
 * In the example below we have {@code rd} and {@code rs1} which are both indexes and have no
 * {@link Format.Field}.
 * <code>
 * pseudo instruction MOV( rd : Index, rs1 : Index ) =
 * {
 * ADDI{ rd = rd, rs1 = rs1, imm = 0 as Bits12 }
 * }
 * </code>
 */
public class GcbInstructionIndexedRegisterFileOperand
    extends GcbDefaultInstructionOperand {
  private final RegisterTensor registerFile;

  /**
   * Constructor.
   */
  public GcbInstructionIndexedRegisterFileOperand(ReadRegTensorNode node,
                                                  FuncParamNode address) {
    super(node, node.regTensor().simpleName(), address.parameter().identifier.simpleName());
    this.registerFile = node.regTensor();
    node.regTensor().ensure(node.regTensor().isRegisterFile(), "must be a register file");
  }

  /**
   * Constructor.
   */
  public GcbInstructionIndexedRegisterFileOperand(WriteRegTensorNode node,
                                                  FuncParamNode address) {
    super(node, node.regTensor().simpleName(), address.parameter().identifier.simpleName());
    this.registerFile = node.regTensor();
    node.regTensor().ensure(node.regTensor().isRegisterFile(), "must be a register file");
  }

  /**
   * Constructor.
   */
  public GcbInstructionIndexedRegisterFileOperand(ReadArtificialResNode node,
                                                  FuncParamNode address) {
    super(node, node.resourceDefinition().innerResourceRef().simpleName(),
        address.parameter().identifier.simpleName());
    this.registerFile = (RegisterTensor) node.resourceDefinition().innerResourceRef();
    node.resourceDefinition().innerResourceRef()
        .ensure(registerFile.isRegisterFile(), "must be registerfile");
  }

  /**
   * Constructor.
   */
  public GcbInstructionIndexedRegisterFileOperand(WriteArtificialResNode node,
                                                  FuncParamNode address) {
    super(node, node.resourceDefinition().innerResourceRef().simpleName(),
        address.parameter().identifier.simpleName());
    this.registerFile = (RegisterTensor) node.resourceDefinition().innerResourceRef();
    node.resourceDefinition().innerResourceRef()
        .ensure(registerFile.isRegisterFile(), "must be registerfile");
  }

  public RegisterTensor registerFile() {
    return registerFile;
  }

  @Override
  public int hashCode() {
    return Objects.hash(registerFile, type(), name());
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GcbInstructionIndexedRegisterFileOperand that =
        (GcbInstructionIndexedRegisterFileOperand) o;
    return Objects.equals(registerFile, that.registerFile)
        && Objects.equals(name(), that.name())
        && Objects.equals(type(), that.type());
  }
}
