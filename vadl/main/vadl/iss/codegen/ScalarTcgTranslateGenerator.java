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

package vadl.iss.codegen;

import static vadl.error.DiagUtils.throwNotAllowed;
import static vadl.utils.GraphUtils.getSingleNode;

import vadl.configuration.IssConfiguration;
import vadl.cppCodeGen.context.CGenContext;
import vadl.cppCodeGen.context.CNodeContext;
import vadl.cppCodeGen.mixins.CDefaultMixins;
import vadl.cppCodeGen.mixins.CInvalidMixins;
import vadl.iss.passes.extensions.InstrInfo;
import vadl.iss.passes.nodes.IssRegBitfieldWriteNode;
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.iss.passes.tcgLowering.nodes.TcgNode;
import vadl.javaannotations.DispatchFor;
import vadl.javaannotations.Handler;
import vadl.viam.Instruction;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.AsmBuiltInCall;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FoldNode;

/**
 * Emits direct non-helper translation functions.
 *
 * <p>This generator traverses the instruction's behavior graph and renders TCG operations directly
 * into the generated {@code trans_<instr>} function. It is used for instructions whose selected
 * execution path is `NORMAL_TCG`, or through the legacy fallback when no execution plan is
 * available yet. Lowered gvec backend nodes stay in the same graph and are emitted by the same
 * renderer.</p>
 *
 * <p>Compared to helper-based emission, this path keeps instruction semantics in translated TCG
 * form, which is typically the most efficient runtime path for scalar ALU, control-flow, and other
 * TCG-representable instruction bodies.</p>
 */
@DispatchFor(
    value = Node.class,
    context = CNodeContext.class,
    include = {"vadl.viam", "vadl.iss"}
)
class ScalarTcgTranslateGenerator implements InstructionTranslateGenerator,
    CDefaultMixins.All, IssCMixins.Default, IssCMixins.StaticReadRegTensor,
    CInvalidMixins.SideEffect, CInvalidMixins.ResourceReads,
    CInvalidMixins.InstrCall, CInvalidMixins.HardwareRelated {

  private final Instruction insn;
  private final StringBuilder builder;
  private final CNodeContext ctx;
  private final String targetName;
  private final InstrInfo info;

  ScalarTcgTranslateGenerator(Instruction instr,
                              IssConfiguration configuration) {
    this.insn = instr;
    this.info = instr.expectExtension(InstrInfo.class);
    this.builder = new StringBuilder();
    this.targetName = configuration.targetName();
    this.ctx = new CNodeContext(
        builder::append,
        (dispatchCtx, node) ->
            ScalarTcgTranslateGeneratorDispatcher.dispatch(this, dispatchCtx, node)
    );
  }

  @Override
  public String fetch() {
    ctx.wr("static bool trans_");
    ctx.wr(info.cIdentName());
    ctx.wr("(DisasContext *ctx, arg_");
    ctx.wr(info.cIdentName());
    ctx.ln(" *a) {");

    ctx.spacedIn().ln("trace_" + targetName.toLowerCase() + "_instr_trans(__func__);");

    var start = getSingleNode(insn.behavior(), StartNode.class);
    ctx.gen(start.next());
    ctx.wr("\n\treturn true; \n}\n");

    return builder.toString();
  }

  @Handler
  void impl(CGenContext<Node> ctx, TcgNode node) {
    var c = node.cCode(ctx::genToString).trim();
    if (!c.endsWith(";")) {
      c += ";";
    }
    ctx.ln(c)
        .gen(node.next());
  }

  @Handler
  void impl(CGenContext<Node> ctx, FieldRefNode node) {
    ctx.wr("a->");
    ctx.wr(node.formatField().simpleName());
  }

  @Handler
  void impl(CGenContext<Node> ctx, FieldAccessRefNode node) {
    ctx.wr("a->");
    ctx.wr(node.fieldAccess().simpleName());
  }

  @Handler
  void handle(CGenContext<Node> ctx, TcgVRefNode toHandle) {
    ctx.wr(toHandle.cCode());
  }

  @Handler
  void handle(CGenContext<Node> ctx, AsmBuiltInCall toHandle) {
    throw new UnsupportedOperationException("Type AsmBuiltInCall not allowed");
  }

  @Handler
  void handle(CGenContext<Node> ctx, FoldNode toHandle) {
    throwNotAllowed(toHandle, "forall fold expressions");
  }

  @Handler
  void handle(CGenContext<Node> ctx, IssRegBitfieldWriteNode toHandle) {
    throwNotAllowed(toHandle, "bitfield register writes should be lowered to TCG before codegen");
  }
}
