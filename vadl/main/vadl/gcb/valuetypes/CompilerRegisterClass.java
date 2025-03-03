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
import vadl.viam.Abi;
import vadl.viam.RegisterFile;

/**
 * Extends the concept of the register class for the compiler.
 */
public class CompilerRegisterClass {
  private final String name;
  private final RegisterFile registerFile;
  private final List<CompilerRegister> registers;
  private final Abi.Alignment alignment;

  /**
   * Constructor.
   */
  public CompilerRegisterClass(RegisterFile registerFile,
                               List<CompilerRegister> registers,
                               Abi.Alignment alignment) {
    this.name = registerFile.identifier.simpleName();
    this.registerFile = registerFile;
    this.registers = registers;
    this.alignment = alignment;
  }

  public String name() {
    return name;
  }

  public RegisterFile registerFile() {
    return registerFile;
  }

  public List<CompilerRegister> registers() {
    return registers;
  }

  public Abi.Alignment alignment() {
    return alignment;
  }
}
