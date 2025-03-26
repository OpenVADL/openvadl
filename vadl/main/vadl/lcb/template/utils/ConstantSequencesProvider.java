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

package vadl.lcb.template.utils;

import java.util.Collections;
import java.util.stream.Stream;
import vadl.viam.Abi;
import vadl.viam.CompilerInstruction;
import vadl.viam.Specification;

/**
 * Utility class for getting the constant sequences.
 */
public class ConstantSequencesProvider {
  /**
   * Get the constant sequences.
   */
  public static Stream<CompilerInstruction> getSupportedCompilerInstructions(
      Specification specification) {
    return specification.abi().map(Abi::constantSequences).orElse(Collections.emptyList()).stream();
  }
}
