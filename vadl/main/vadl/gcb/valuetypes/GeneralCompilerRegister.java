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
import vadl.viam.Register;
import vadl.viam.RegisterFile;

/**
 * Like a {@link CompilerRegister} but it is the concrete implementation which are not indexed.
 * This distinction is important since not all {@link CompilerRegister} are indexed e.g. PC.
 * A {@link GeneralCompilerRegister} is exactly for registers like PC which are not defined over a
 * {@link RegisterFile}.
 */
public class GeneralCompilerRegister extends CompilerRegister {
  public GeneralCompilerRegister(Register register,
                                 String asmName,
                                 List<String> altNames,
                                 int dwarfNumber) {
    super(generateName(register), asmName, altNames, dwarfNumber, 0);
  }

  /**
   * Generate the internal compiler name from a {@link Register}.
   */
  public static String generateName(Register register) {
    return register.simpleName();
  }
}
