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

package vadl.iss.passes.extensions;

import java.util.List;
import java.util.Map;
import vadl.template.Renderable;

/**
 * Holds the extracted ABI register indices for User-Mode Emulation.
 * Maps the software calling convention to the raw hardware registers.
 */
public record UmeInfo(
    int sysReg,          // e.g., 17 for RISC-V a7 (syscall number)
    int retReg,          // e.g., 10 for RISC-V a0 (return value)
    List<Integer> args   // e.g., [10, 11, 12, 13, 14, 15] for a0-a5 (syscall arguments)
) implements Renderable {

  @Override
  public Map<String, Object> renderObj() {
    return Map.of(
        "sysReg", sysReg,
        "retReg", retReg,
        "args", args
    );
  }
}
