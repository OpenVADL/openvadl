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

package vadl.lcb.passes.llvmLowering.tablegen.model.register;

import java.util.List;
import vadl.gcb.valuetypes.TargetName;
import vadl.gcb.valuetypes.ValueType;
import vadl.viam.ArtificialResource;
import vadl.viam.RegisterTensor;


/**
 * Represents the alias of a single register file in TableGen. This is the lowered representation
 * of a register file.
 */
public class TableGenAliasRegisterClass extends TableGenRegisterClass {
  /**
   * Constructor.
   */
  public TableGenAliasRegisterClass(TargetName namespace,
                                    String name,
                                    int alignment,
                                    List<ValueType> regTypes,
                                    List<TableGenRegister> registers,
                                    ArtificialResource registerFileRef) {
    super(namespace, name, alignment, regTypes, registers, registerFileRef);
  }

  /**
   * Get the reference to the register file that is referenced.
   */
  public RegisterTensor underlyingRegisterFile() {
    var ref = (ArtificialResource) registerFileRef();
    return (RegisterTensor) ref.innerResourceRef();
  }
}
