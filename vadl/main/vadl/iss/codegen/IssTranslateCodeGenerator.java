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

package vadl.iss.codegen;

import static vadl.error.DiagUtils.throwNotAllowed;
import static vadl.utils.GraphUtils.getSingleNode;

import vadl.configuration.IssConfiguration;
import vadl.cppCodeGen.context.CGenContext;
import vadl.cppCodeGen.context.CNodeContext;
import vadl.cppCodeGen.mixins.CDefaultMixins;
import vadl.cppCodeGen.mixins.CInvalidMixins;
import vadl.iss.passes.nodes.IssStaticPcRegNode;
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
import vadl.viam.graph.dependency.TensorNode;

/**
 * The code generator for the {@code target/gen-arch/translate.c}.
 * It produces translate functions for all instructions
 * in the {@link vadl.viam.InstructionSetArchitecture}.
 */
@DispatchFor(
    value = Node.class,
    context = CNodeContext.class,
    include = {"vadl.viam", "vadl.iss"}
)
public class IssTranslateCodeGenerator implements
    // default implementations
    CDefaultMixins.All, IssCMixins.Default,
    // invalid nodes
    CInvalidMixins.SideEffect, CInvalidMixins.ResourceReads,
    CInvalidMixins.InstrCall, CInvalidMixins.HardwareRelated {

  private Instruction insn;
  private StringBuilder builder;
  private CNodeContext ctx;
  private String targetName;

  /**
   * Constructs IssTranslateCodeGenerator.
   */
  public IssTranslateCodeGenerator(Instruction instr,
                                   IssConfiguration configuration) {
    this.insn = instr;
    this.builder = new StringBuilder();
    this.targetName = configuration.targetName();
    this.ctx = new CNodeContext(
        builder::append,
        (ctx, node)
            -> IssTranslateCodeGeneratorDispatcher.dispatch(this, ctx, node)
    );
  }


  /**
   * The static entry point to get the translation function for a given instruction.
   */
  public static String fetch(Instruction def,
                             IssConfiguration configuration) {
    var generator = new IssTranslateCodeGenerator(def, configuration);
    return generator.fetch();
  }

  private String fetch() {

    var name = insn.identifier.simpleName().toLowerCase();
    // static bool trans_<name>(DisasContext *ctx, arg_<name> *a) {\n
    ctx.wr("static bool trans_");
    ctx.wr(name);
    ctx.wr("(DisasContext *ctx, arg_");
    ctx.wr(name);
    ctx.ln(" *a) {");

    ctx.spacedIn().ln("trace_" + this.targetName.toLowerCase() + "_instr_trans(__func__);");

    var start = getSingleNode(insn.behavior(), StartNode.class);
    var current = start.next();

    ctx.gen(current);
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
  void impl(CGenContext<Node> ctx, IssStaticPcRegNode node) {
    ctx.wr("(ctx->pc_curr)");
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
  void handle(CGenContext<Node> ctx, TensorNode toHandle) {
    throwNotAllowed(toHandle, "forall tensor expressions");
  }

}
