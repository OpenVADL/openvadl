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

package vadl.cppCodeGen.mixins;

import java.util.stream.Collectors;
import vadl.cppCodeGen.context.CGenContext;
import vadl.cppCodeGen.model.nodes.CppUpdateBitRangeNode;
import vadl.javaannotations.Handler;
import vadl.types.BitsType;
import vadl.viam.graph.Node;

/**
 * Code generation mixins for relocations.
 */
public interface CRelocationMixins extends CDefaultMixins.Utils {
  /**
   * Generate code for {@link CppUpdateBitRangeNode}.
   */
  @Handler
  default void handle(CGenContext<Node> ctx, CppUpdateBitRangeNode toHandle) {
    var bitWidth = ((BitsType) toHandle.type()).bitWidth();
    ctx.wr("set_bits(");

    // Inst
    ctx.wr(String.format("std::bitset<%d>(", bitWidth));
    ctx.gen(toHandle.value);
    ctx.wr("), ");

    // New value
    ctx.wr(String.format("std::bitset<%d>(", bitWidth));
    ctx.gen(toHandle.patch);
    ctx.wr(")");

    // Parts
    ctx.wr(", std::vector<int> { ");
    ctx.wr(toHandle.field.bitSlice()
        .stream()
        .mapToObj(String::valueOf)
        .collect(Collectors.joining(", ")));
    ctx.wr(" } ");
    ctx.wr(").to_ulong()");
  }
}
