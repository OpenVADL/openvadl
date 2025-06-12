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

package vadl.gcb.passes.encodingGeneration.strategies;

import vadl.viam.Format;
import vadl.viam.PrintableInstruction;
import vadl.viam.graph.Graph;

/**
 * The implementor of this interface can generate a field access encoding function.
 */
public interface EncodingGenerationStrategy {
  /**
   * Check if the strategy can be applied. Returns {@code true} when it is applicable.
   */
  boolean checkIfApplicable(Format.FieldAccess fieldAccess);

  /**
   * Create the inverse behavior graph of a field access function.
   * It also adds the created nodes to {@code vadl.viam.Format.FieldAccess#encoding}.
   */
  void generateEncoding(PrintableInstruction instruction, Format.FieldAccess fieldAccess);

  /**
   * Creates a new {@link vadl.viam.Format.FieldEncoding} for the given field
   * and the behavior graph.
   * It assumes that there is only a single field references by the field access.
   */
  default void setFieldEncoding(PrintableInstruction instruction,
                                Format.FieldAccess fieldAccess,
                                Graph behavior) {
    var ident = fieldAccess.identifier.last().prepend(instruction.identifier());
    var format = fieldAccess.format();
    var encoding = new Format.FieldEncoding(ident, fieldAccess.fieldRef(), behavior);
    format.setFieldEncoding(encoding);
  }
}
