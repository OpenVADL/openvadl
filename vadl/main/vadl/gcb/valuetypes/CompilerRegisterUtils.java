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

package vadl.gcb.valuetypes;

import java.util.List;
import java.util.Optional;
import vadl.viam.Constant;
import vadl.viam.RegisterResource;
import vadl.viam.ViamError;

/**
 * Utility methods for compiler-facing register names and register-file conventions.
 */
public final class CompilerRegisterUtils {

  private CompilerRegisterUtils() {
  }

  /**
   * Generates the compiler register name for an indexed register-file entry.
   */
  public static String indexedRegisterName(RegisterResource registerFile, int index) {
    ViamError.ensure(registerFile.isRegisterFile(), "must be registerFile");
    return registerFile.identifier().simpleName() + index;
  }

  /**
   * Returns the index tuple of a zero register if the register file defines one.
   */
  public static Optional<List<Constant.Value>> zeroRegister(RegisterResource registerFile) {
    return registerFile.constraints()
        .stream()
        .filter(c -> c.value().intValue() == 0)
        .map(RegisterResource.Constraint::indices)
        .findFirst();
  }
}
