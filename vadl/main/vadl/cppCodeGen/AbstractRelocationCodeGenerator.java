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

package vadl.cppCodeGen;

import java.util.stream.Collectors;
import vadl.cppCodeGen.common.UpdateFieldRelocationFunctionCodeGenerator;
import vadl.cppCodeGen.common.ValueRelocationFunctionCodeGenerator;
import vadl.cppCodeGen.context.CGenContext;
import vadl.cppCodeGen.context.CNodeContext;
import vadl.cppCodeGen.mixins.CDefaultMixins;
import vadl.cppCodeGen.model.nodes.CppUpdateBitRangeNode;
import vadl.javaannotations.DispatchFor;
import vadl.javaannotations.Handler;
import vadl.types.BitsType;
import vadl.types.DataType;
import vadl.viam.Function;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.TruncateNode;

/**
 * This class overrides the default implementation of {@link CDefaultMixins} for
 * generating code for {@link ValueRelocationFunctionCodeGenerator} and
 * {@link UpdateFieldRelocationFunctionCodeGenerator}.
 */
@DispatchFor(
    value = ExpressionNode.class,
    context = CNodeContext.class,
    include = "vadl.cppCodeGen.model.nodes"
)
public abstract class AbstractRelocationCodeGenerator extends AbstractFunctionCodeGenerator {
  public AbstractRelocationCodeGenerator(Function function) {
    super(function);
  }

  @Override
  public void handle(CGenContext<Node> ctx, TruncateNode node) {
    node.ensure(node.type() != DataType.bool(),
        "Truncation to boolean is not allowed");
    var bitWidth = node.type().bitWidth();

    ctx.wr("VADL_uextract(")
        .gen(node.value())
        .wr(", %s)", bitWidth);
  }

  /**
   * Generate code for {@link CppUpdateBitRangeNode}.
   */
  @Handler
  public void handle(CGenContext<Node> ctx, CppUpdateBitRangeNode toHandle) {
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

  @Override
  public void handle(CGenContext<Node> ctx, SliceNode toHandle) {
    var parts = toHandle.bitSlice().parts().toList();
    ctx.wr("(");

    int acc = 0;
    for (int i = parts.size() - 1; i >= 0; i--) {
      if (i != parts.size() - 1) {
        ctx.wr(" | ");
      }

      var part = parts.get(i);
      var bitWidth = ((BitsType) toHandle.value().type()).bitWidth();
      if (part.isIndex()) {
        ctx.wr(
            String.format("project_range<%d, %d>(std::bitset<%d>(", part.lsb(), part.msb(),
                bitWidth));
        ctx.gen(toHandle.value()); // same expression
        ctx.wr(String.format(")) << %d", acc));
      } else {
        ctx.wr(
            String.format("project_range<%d, %d>(std::bitset<%d>(", part.lsb(), part.msb(),
                bitWidth));
        ctx.gen(toHandle.value()); // same expression
        ctx.wr(String.format(")) << %d", acc));
      }

      acc += part.msb() - part.lsb() + 1;
    }
    ctx.wr(").to_ulong()");
  }
}
