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
import static vadl.iss.passes.TcgPassUtils.instrInfo;
import static vadl.utils.GraphUtils.getSingleNode;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import vadl.configuration.IssConfiguration;
import vadl.cppCodeGen.context.CGenContext;
import vadl.cppCodeGen.context.CNodeContext;
import vadl.cppCodeGen.mixins.CDefaultMixins;
import vadl.cppCodeGen.mixins.CInvalidMixins;
import vadl.iss.passes.extensions.InstrInfo;
import vadl.iss.passes.nodes.IssStaticPcRegNode;
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.iss.passes.tcgLowering.nodes.TcgNode;
import vadl.javaannotations.DispatchFor;
import vadl.javaannotations.Handler;
import vadl.utils.codegen.CodeGeneratorAppendable;
import vadl.utils.codegen.StringBuilderAppendable;
import vadl.viam.Instruction;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.AsmBuiltInCall;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FoldNode;
import vadl.viam.graph.dependency.ParamNode;
import vadl.viam.graph.dependency.TensorNode;

/**
 * The code generator for the {@code target/gen-arch/translate.c}.
 * It produces translate functions for all instructions
 * in the {@link vadl.viam.InstructionSetArchitecture}.
 *
 * <p>Depending on the complexity of the instruction, the generated code will be
 * either produced by the {@link DefaultGenerator} or the {@link HelperGenerator}.
 * While the default generator will use only TCG operations, the helper generator
 * will use a helper function that executes the instruction
 */
public class IssTranslateCodeGenerator {

  /**
   * The static entry point to get the translation function for a given instruction.
   */
  public static String fetch(Instruction def,
                             IssConfiguration configuration) {
    if (instrInfo(def).asHelperCall()) {
      return new HelperGenerator(def, configuration).fetch();
    } else {
      return new DefaultGenerator(def, configuration).fetch();
    }
  }
}

@DispatchFor(
    value = Node.class,
    context = CNodeContext.class,
    include = {"vadl.viam", "vadl.iss"}
)
class DefaultGenerator implements
    // default implementations
    CDefaultMixins.All, IssCMixins.Default,
    // invalid nodes
    CInvalidMixins.SideEffect, CInvalidMixins.ResourceReads,
    CInvalidMixins.InstrCall, CInvalidMixins.HardwareRelated {

  private Instruction insn;
  private StringBuilder builder;
  private CNodeContext ctx;
  private String targetName;
  private InstrInfo info;

  /**
   * Constructs DefaultGenerator.
   */
  DefaultGenerator(Instruction instr,
                   IssConfiguration configuration) {
    this.insn = instr;
    this.info = instr.expectExtension(InstrInfo.class);
    this.builder = new StringBuilder();
    this.targetName = configuration.targetName();
    this.ctx = new CNodeContext(
        builder::append,
        (ctx, node)
            -> DefaultGeneratorDispatcher.dispatch(this, ctx, node)
    );
  }

  String fetch() {

    // static bool trans_<name>(DisasContext *ctx, arg_<name> *a) {\n
    ctx.wr("static bool trans_");
    ctx.wr(info.cIdentName());
    ctx.wr("(DisasContext *ctx, arg_");
    ctx.wr(info.cIdentName());
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

@SuppressWarnings("unused")
class HelperGenerator {

  private Instruction insn;
  private CodeGeneratorAppendable builder;
  private String targetName;
  private InstrInfo info;

  /**
   * Constructs DefaultGenerator.
   */
  HelperGenerator(Instruction instr,
                  IssConfiguration configuration) {
    this.insn = instr;
    this.info = instrInfo(instr);
    this.builder = new StringBuilderAppendable();
    this.targetName = configuration.targetName();
  }

  String fetch() {

    // static bool trans_<name>(DisasContext *ctx, arg_<name> *a) {\n
    builder.append("static bool trans_")
        .append(info.cIdentName())
        .append("(DisasContext *ctx, arg_")
        .append(info.cIdentName())
        .appendLn(" *a) {")
        .indent();

    genArgTcgVs();
    genHelperCall();

    builder.appendLn("return true;")
        .unindent()
        .appendLn("}");
    return builder.toString();
  }

  private void genArgTcgVs() {
    info.helperFormatParamOrder()
        .forEach(p -> {
          var name = paramName(p);
          var val = "a->" + name;
          var stmt = "TCGv_i32 " + name + "_tmp = tcg_constant_i32(" + val + ");";
          builder.appendLn(stmt);
        });
  }

  private void genHelperCall() {
    var args = Stream.concat(Stream.of("tcg_env"),
        fieldArgs()).collect(Collectors.joining(", "));
    var call = "gen_helper_"
        + info.cIdentName()
        + "(" + args + ")";
    builder.appendLn(call);
  }

  private Stream<String> fieldArgs() {
    return info.helperFormatParamOrder()
        .map(p -> paramName(p) + "_tmp");
  }

  private String paramName(ParamNode p) {
    return p.definition().simpleName().toLowerCase();
  }

}