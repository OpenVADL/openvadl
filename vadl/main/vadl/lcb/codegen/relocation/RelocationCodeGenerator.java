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

package vadl.lcb.codegen.relocation;

import static vadl.error.DiagUtils.throwNotAllowed;

import java.util.stream.Collectors;
import vadl.cppCodeGen.context.CGenContext;
import vadl.cppCodeGen.context.CNodeContext;
import vadl.cppCodeGen.mixins.CDefaultMixins;
import vadl.cppCodeGen.mixins.CInvalidMixins;
import vadl.cppCodeGen.model.nodes.CppUpdateBitRangeNode;
import vadl.javaannotations.DispatchFor;
import vadl.javaannotations.Handler;
import vadl.types.BitsType;
import vadl.viam.Function;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.AsmBuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.ReadArtificialResNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.ReadRegNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.SliceNode;

/**
 * Code generator to generate the extraction of immediates and updating of immediates in a
 * binary machine instruction.
 */
@DispatchFor(
    value = ExpressionNode.class,
    context = CNodeContext.class,
    include = {"vadl.viam", "vadl.cppCodeGen.model.nodes"}
)
public class RelocationCodeGenerator
    implements CDefaultMixins.AllExpressions, CDefaultMixins.Utils, CInvalidMixins.ReadStageOutput {
  protected final Function function;
  protected final CNodeContext context;
  protected final StringBuilder builder;

  /**
   * Creates a new code generator for the specified function.
   *
   * @param function the function for which code should be generated
   */
  public RelocationCodeGenerator(Function function) {
    this.function = function;
    this.builder = new StringBuilder();
    this.context = new CNodeContext(
        builder::append,
        (ctx, node)
            -> RelocationCodeGeneratorDispatcher.dispatch(this, ctx, (ExpressionNode) node)
    );
  }

  @Handler
  protected void handle(CGenContext<Node> ctx, ReadRegNode toHandle) {
    throwNotAllowed(toHandle, "Register reads");
  }

  @Handler
  protected void handle(CGenContext<Node> ctx, ReadRegFileNode toHandle) {
    throwNotAllowed(toHandle, "Register reads");
  }

  @Handler
  protected void handle(CGenContext<Node> ctx, ReadRegTensorNode toHandle) {
    throwNotAllowed(toHandle, "Register reads");
  }

  @Handler
  protected void handle(CGenContext<Node> ctx, ReadArtificialResNode toHandle) {
    throwNotAllowed(toHandle, "Resource reads");
  }

  @Handler
  protected void handle(CGenContext<Node> ctx, ReadMemNode toHandle) {
    throwNotAllowed(toHandle, "Memory reads");
  }

  @Handler
  @Override
  public void handle(CGenContext<Node> ctx, FuncParamNode toHandle) {
    ctx.wr(toHandle.parameter().simpleName());
  }

  @Handler
  protected void handle(CGenContext<Node> ctx, FieldAccessRefNode toHandle) {
    throwNotAllowed(toHandle, "Format field accesses");
  }

  @Handler
  protected void handle(CGenContext<Node> ctx, FieldRefNode toHandle) {
    throwNotAllowed(toHandle, "Format field accesses");
  }

  @Handler
  protected void handle(CGenContext<Node> ctx, AsmBuiltInCall toHandle) {
    throwNotAllowed(toHandle, "Asm builtin calls");
  }

  @Handler
  protected void handle(CGenContext<Node> ctx, CppUpdateBitRangeNode toHandle) {
    var bitWidth = ((BitsType) toHandle.type()).bitWidth();
    ctx.wr("set_bits(");

    // Inst
    ctx.wr("std::bitset<%d>(", bitWidth);
    ctx.gen(toHandle.value);
    ctx.wr("), ");

    // New value
    ctx.wr("std::bitset<%d>(", bitWidth);
    ctx.gen(toHandle.patch);
    ctx.wr(")");

    // Parts
    ctx.wr(", std::vector<int> { ");
    ctx.wr(toHandle.field.bitSlice()
            .stream()
            .mapToObj(String::valueOf)
            .collect(Collectors.joining(", ")))
        .wr(" } ");

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
      ctx.wr(
          "project_range<%d, %d>(std::bitset<%d>(", part.lsb(), part.msb(),
          bitWidth);
      ctx.gen(toHandle.value());
      ctx.wr(")) << %d", acc);

      acc += part.msb() - part.lsb() + 1;
    }
    ctx.wr(").to_ulong()");
  }

  @Override
  public CNodeContext context() {
    return context;
  }

  @Override
  public Function function() {
    return function;
  }

  @Override
  public StringBuilder builder() {
    return builder;
  }

  @Override
  public String genFunctionName() {
    return function.identifier.lower();
  }
}
