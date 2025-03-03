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

import vadl.viam.Encoding;
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.Parameter;

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
  void generateEncoding(Format.FieldAccess fieldAccess);

  /**
   * Creates a new function for {@link Encoding}. This function has side effects for the
   * {@code fieldAccess}.
   *
   * @param fieldAccess for which the encoding should be generated.
   * @return the {@link Parameter} which is the input for the encoding function.
   */
  default Parameter setupEncodingForFieldAccess(Format.FieldAccess fieldAccess) {
    var ident = fieldAccess.identifier.append("encoding");
    var identParam = ident.append(fieldAccess.simpleName());
    var param = new Parameter(identParam, fieldAccess.accessFunction().returnType());
    var function =
        new Function(ident, new Parameter[] {param}, fieldAccess.fieldRef().type());
    param.setParent(function);

    fieldAccess.setEncoding(function);
    return param;
  }
}
