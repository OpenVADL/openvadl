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

package vadl.iss.template;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import vadl.iss.passes.extensions.RegInfo;
import vadl.utils.Pair;
import vadl.utils.codegen.CodeGeneratorAppendable;
import vadl.viam.Specification;

/**
 * A set of utils to help render ISS templates.
 */
public class IssRenderUtils {

  /**
   * Maps given {@link Specification} to a list of {@link RegInfo}, which can be rendered.
   */
  public static List<RegInfo> mapRegTensors(Specification spec) {
    return spec.isa().get()
        .registerTensors()
        .stream().map(reg -> reg.expectExtension(RegInfo.class))
        .toList();
  }

  /**
   * Maps given {@link Specification} to the register mapping of the ISA's PC.
   */
  public static RegInfo mapPc(Specification spec) {
    return Objects.requireNonNull(spec.isa().get()
            .pc()).registerTensor()
        .expectExtension(RegInfo.class);
  }


  /**
   * A helper function that generates a nested C loop for the provided layers.
   *
   * @param sb          the code builder to generate the C code.
   * @param layers      one layer per loop, containing the running var name and the upper integer
   *                    bound
   * @param bodyBuilder a function that appends c code as body to the code generator
   */
  public static void generateNestedLoops(CodeGeneratorAppendable sb,
                                         List<Pair<String, Integer>> layers,
                                         Consumer<CodeGeneratorAppendable> bodyBuilder) {
    for (Pair<String, Integer> layer : layers) {
      sb.append("for(int ").append(layer.left()).append(" = 0; ")
          .append(layer.left()).append(" < ").append(layer.right()).append("; ")
          .append(layer.left()).appendLn("++) {")
          .indent();
    }
    bodyBuilder.accept(sb);
    sb.appendLn("");
    for (Pair<String, Integer> ignored : layers) {
      sb.unindent().appendLn("}");
    }
  }

}
