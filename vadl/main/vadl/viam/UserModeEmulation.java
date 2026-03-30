// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

import java.util.List;
import java.util.Map;

public class UserModeEmulation extends Definition {
  private final int sysReg;          // e.g., 17 for RISC-V a7 (syscall number)
  private final int retReg;          // e.g., 10 for RISC-V a0 (return value)
  private final int spReg;           // Stack Pointer (e.g., 2 for RISC-V)
  private final int raReg;           // Return Address (e.g., 1 for RISC-V)
  private final int tpReg;           // Thread Pointer
  private final List<Integer> args;  // Argument registers (e.g., [10, 11, 12, 13, 14, 15])
  private final Map<String, Integer> excIds; // Exception IDs mapping

  /**
   * Constructs a UserModeEmulation configuration.
   */
  public UserModeEmulation(
      Identifier identifier,
      int sysReg,
      int retReg,
      int spReg,
      int raReg,
      int tpReg,
      List<Integer> args,
      Map<String, Integer> excIds) {
    super(identifier);
    this.sysReg = sysReg;
    this.retReg = retReg;
    this.spReg = spReg;
    this.raReg = raReg;
    this.tpReg = tpReg;
    this.args = args;
    this.excIds = excIds;
  }

  public int sysReg() {
    return sysReg;
  }

  public int retReg() {
    return retReg;
  }

  public int spReg() {
    return spReg;
  }

  public int raReg() {
    return raReg;
  }

  public int tpReg() {
    return tpReg;
  }

  public List<Integer> args() {
    return args;
  }

  public Map<String, Integer> excIds() {
    return excIds;
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    return simpleName() + " [sysReg=" + sysReg + ", retReg=" + retReg + ", spReg=" + spReg
        + ", raReg=" + raReg + ", tpReg=" + tpReg + ", args=" + args + ", excIds=" + excIds + "]";
  }
}
