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

package vadl.gcb.valuetypes;

import java.util.List;
import vadl.viam.ArtificialResource;
import vadl.viam.RegisterTensor;

/**
 * Like a {@link CompilerRegister} but it is the concrete implementation which are not indexed.
 * This distinction is important since not all {@link CompilerRegister} are indexed e.g. PC.
 * A {@link GeneralCompilerRegister} is exactly for registers like PC which are not defined over a
 * register file.
 */
public class GeneralCompilerRegister extends CompilerRegister {
  /**
   * Generate a register from a {@link RegisterTensor}. It has no {@code hwEncodingValue} because
   * it is a separate physical register.
   */
  public GeneralCompilerRegister(RegisterTensor register,
                                 String asmName,
                                 List<String> altNames,
                                 int dwarfNumber) {
    super(generateName(register), asmName, altNames, dwarfNumber, 0, false, register);
  }

  /**
   * Generate a register which is an alias to a physical register or a register file. This
   * constructor handles a register file because it takes {@code address} as input.
   */
  public GeneralCompilerRegister(ArtificialResource artificialResource,
                                 int address, /* hwEncoding */
                                 String asmName,
                                 List<String> altNames,
                                 int dwarfNumber) {
    super(generateName(artificialResource), asmName, altNames, dwarfNumber, address, true,
        artificialResource);
  }

  /**
   * Generate a register which is an alias to a physical register or a register file. This
   * constructor handles an alias for a physical register because the {@code hwEncodingValue} is
   * zero.
   */
  public GeneralCompilerRegister(ArtificialResource artificialResource,
                                 String asmName,
                                 List<String> altNames,
                                 int dwarfNumber) {
    super(generateName(artificialResource), asmName, altNames, dwarfNumber, 0, true,
        artificialResource);
  }

  /**
   * Generate the internal compiler name from a register.
   */
  public static String generateName(RegisterTensor register) {
    register.ensure(register.isSingleRegister(), "must be single register");
    return register.simpleName();
  }

  /**
   * Generate the internal compiler name from a register.
   */
  public static String generateName(ArtificialResource artificialResource) {
    var registerTensor = (RegisterTensor) artificialResource.innerResourceRef();

    // Either is the underlying a register or the readFunction has no inputs.
    artificialResource.ensure(registerTensor.isSingleRegister()
        || artificialResource.readFunction().parameters().length == 0, "must be single register");
    return artificialResource.simpleName();
  }
}
