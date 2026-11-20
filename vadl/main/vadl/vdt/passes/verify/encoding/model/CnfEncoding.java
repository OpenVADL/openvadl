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

package vadl.vdt.passes.verify.encoding.model;

import java.util.List;
import vadl.vdt.impl.irregular.model.DecodeEntry;

/**
 * The CNF encoding of the encoding definitions.
 */
public record CnfEncoding(List<DecodeEntry> entries,
                          VariablePool pool,
                          List<Integer> bitVariables,
                          List<Clause> clauses) {

  /**
   * Convert the CNF encoding to DIMACS format.
   */
  public String toDimacs() {
    StringBuilder sb = new StringBuilder();
    sb.append("p cnf ").append(pool.size()).append(" ").append(clauses.size()).append("\n");
    for (Clause clause : clauses) {
      for (Integer literal : clause.literals()) {
        sb.append(literal).append(" ");
      }
      sb.append("0\n");
    }
    return sb.toString();
  }
}