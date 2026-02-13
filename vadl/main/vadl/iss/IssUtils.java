// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.iss;

import static vadl.iss.passes.TcgPassUtils.regInfo;

import com.google.errorprone.annotations.FormatMethod;
import java.util.List;
import vadl.error.Diagnostic;
import vadl.viam.RegisterTensor;
import vadl.viam.graph.Node;

/**
 * Iss specific utility methods.
 * Some more TCG related methods can be found in {@link vadl.iss.passes.TcgPassUtils}.
 */
public class IssUtils {

  /**
   * Throws a user-readable internal error with a call to report the issue.
   */
  @FormatMethod
  @SuppressWarnings("DoNotCallSuggester")
  public static void internalError(Node node, String format, Object... args) {
    throw Diagnostic.error("INTERNAL ERROR", node)
        .description(format, args)
        .help("Please report this issue at https://github.com/openvadl/openvadl/issues")
        .build();
  }

  public static boolean isTcgReg(RegisterTensor registerTensor) {
    return regInfo(registerTensor).isTcgScalar();
  }

  /**
   * Returns a C expression that calculates the index of an innermost dimension register
   * with in the register tensor in the cpu state.
   *
   * <p>An example:
   * <pre>{@code
   * register X: Bits<2><3><32>
   *
   * // cpu.c register
   * uint32_t reg_x[6];
   *
   * // to access the register regX(i0, i1) the calculation is
   * fun regX(i0, i1):
   *    // i0 * 3 because the second dimension has 5 elements
   *    reg_x[i0 * 3 + i1]
   *
   * // example current state reg_x = [1,2,3,4,5,6]
   * regX(1, 1) ... reg_x[0 * 3 + 1] = 2
   * }</pre>
   *
   * @param indexVars index variables (e.g. i0, i1)
   * @param reg       the register tensor
   * @return the calculation based on the above method, e.g. {@code "i0 * 2 + i1"}
   */
  @SuppressWarnings("MethodName")
  public static String cIndex(List<String> indexVars, RegisterTensor reg) {
    var dims = reg.indexDimensions();
    reg.ensure(dims.size() == indexVars.size(),
        "Invalid number of provided indices. Expected %s, but got %s.",
        dims.size(), indexVars.size());

    var sb = new StringBuilder();

    for (int i = 0; i < indexVars.size(); i++) {
      if (i > 0) {
        sb.append(" + ");
      }

      // Reverse the index: (dimSize - 1 - indexVar)
      sb.append("(").append(indexVars.get(i));

      if (i != indexVars.size() - 1) {
        // If we are not at the last index dimension we must multiply the index by the
        // innermost dimension size.
        // Get inner dimension sizes, without the innermost dimension size, as it is the
        // c value type of the C Array (like uint32[6] in the documentation example).
        // Multiply by the product of subsequent dimension sizes
        var innerDimensionSizes = dims.stream()
            .mapToInt(RegisterTensor.Dimension::size)
            .skip(i + 1)
            .reduce(1, (a, b) -> a * b);
        sb.append(" * ").append(innerDimensionSizes);
      }

      sb.append(")");
    }

    return sb.toString();
  }

}
