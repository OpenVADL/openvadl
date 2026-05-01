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

package vadl.iss.codegen;

import static vadl.error.DiagUtils.throwNotAllowed;
import static vadl.iss.passes.TcgPassUtils.instrInfo;
import static vadl.utils.GraphUtils.getSingleNode;

import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
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
import vadl.utils.codegen.CodeGeneratorAppendable;
import vadl.utils.codegen.StringBuilderAppendable;
import vadl.viam.Instruction;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.AsmBuiltInCall;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FoldNode;
import vadl.viam.graph.dependency.ForAllThenNode;
import vadl.viam.graph.dependency.ParamNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;

/**
 * The code generator for the {@code target/gen-arch/translate.c}.
 * It produces translate functions for all instructions
 * in the {@link vadl.viam.InstructionSetArchitecture}.
 *
 * <p>Depending on the complexity of the instruction, the generated code will be
 * either produced by the {@link DefaultGenerator} or the {@link HelperCallGenerator}.
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
      return new HelperCallGenerator(def, configuration).fetch();
    } else {
      return new DefaultGenerator(def, configuration).fetch();
    }
  }
}

///
/// ## DefaultGenerator
///
/// Generates translation code using TCG operations directly.
///
/// The `DefaultGenerator` traverses the instruction's behavior graph and
/// emits TCG (Tiny Code Generator) operations that will be compiled into efficient
/// target machine code. This approach provides better runtime performance as the
/// instruction semantics are directly translated to the target architecture.
///
/// ### How it works
///
/// 1. Walks through the instruction's VIAM graph starting from the `StartNode`
/// 2. Dispatches each node to appropriate handlers that emit corresponding C/TCG code
/// 3. Generates TCG operations like `tcg_gen_add_i32`, `tcg_gen_mov_i32`, etc.
///
/// ### Key Difference from HelperCallGenerator
///
/// - **DefaultGenerator** directly emits TCG operations (e.g., `tcg_gen_add_i32`,
///   `tcg_gen_mov_i32`) that are compiled ahead-of-time into efficient code.
/// - **HelperCallGenerator** generates a simple call to a helper function,
///   with the actual instruction logic executed at runtime through the helper.
///
/// ### When to use
///
/// Used for "simple" instructions where the behavior can be fully expressed using TCG operations:
/// - Basic arithmetic operations (add, sub, mul, div)
/// - Logical operations (and, or, xor)
/// - Register moves and loads/stores
///
/// The decision is made based on `InstrInfo.asHelperCall()` returning `false`.
///
/// ### Generated Code Example
///
/// For a VADL instruction definition:
/// ```vadl
/// instruction ADD : Rtype = X(rd) := ((X(rs1) + X(rs2))) as Regs
/// ```
///
/// The `DefaultGenerator` produces:
/// ```c
/// static bool trans_add(DisasContext *ctx, arg_add *a) {
///     trace_riscv_instr_trans(__func__);
///     TCGv_i64 regfile_x_rd_dest = dest_x(ctx, a->rd);
///     TCGv_i64 regfile_x_rs2 = get_x(ctx, a->rs2);
///     TCGv_i64 regfile_x_rs1 = get_x(ctx, a->rs1);
///     tcg_gen_add_i64(regfile_x_rd_dest, regfile_x_rs1, regfile_x_rs2);
///     return true;
/// }
/// ```
///
/// @see HelperCallGenerator
/// @see InstrInfo#asHelperCall()
///
@DispatchFor(
    value = Node.class,
    context = CNodeContext.class,
    include = {"vadl.viam", "vadl.iss"}
)
class DefaultGenerator implements
    // default implementations
    CDefaultMixins.All, IssCMixins.Default, IssCMixins.StaticReadRegTensor,
    // invalid nodes
    CInvalidMixins.SideEffect, CInvalidMixins.ResourceReads,
    CInvalidMixins.InstrCall, CInvalidMixins.HardwareRelated {

  private Instruction insn;
  private StringBuilder builder;
  private CNodeContext ctx;
  private String targetName;
  private InstrInfo info;

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
  void handle(CGenContext<Node> ctx, ForAllThenNode toHandle) {
    throwNotAllowed(toHandle, "forall then expressions");
  }

  @Handler
  void handle(CGenContext<Node> ctx, IssRegBitfieldWriteNode toHandle) {
    throwNotAllowed(toHandle, "bitfield register writes should be lowered to TCG before codegen");
  }
}

///
/// ## HelperCallGenerator
///
/// Generates translation code that delegates instruction execution to a helper function.
///
/// The `HelperCallGenerator` is used for instructions that are too complex
/// to be efficiently translated into TCG operations. Instead of directly emitting
/// TCG operations in the translate function, this generator produces code that:
/// - Wraps instruction format arguments into TCG temporary variables
/// - Calls a generated helper function (`gen_helper_<name>`) to execute
///   the instruction's behavior
///
/// ### How it works
///
/// 1. Wraps instruction format arguments into TCG temporary variables
/// 2. Calls a generated helper function (`gen_helper_<name>`) to execute the instruction's behavior
/// 3. The helper function contains the full instruction semantics and is executed at runtime
///
/// ### Key Difference from DefaultGenerator
///
/// - **HelperCallGenerator** produces a simple wrapper that delegates to
///   a helper function. The helper function contains the full instruction semantics
///   and is executed at runtime.
/// - **DefaultGenerator** directly emits TCG operations that are then
///   compiled into the target architecture's code, resulting in more efficient
///   execution but limited to TCG-representable operations.
///
/// The decision is made based on `InstrInfo.asHelperCall()`, which determines
/// if an instruction's complexity necessitates helper delegation.
///
/// ### Trade-offs
///
/// **Advantages:**
/// - Can handle any VADL instruction, no matter how complex
/// - No limitations on supported operations
///
/// **Disadvantages:**
/// - Lower runtime performance compared to direct TCG (runtime execution vs. compile-time)
/// - Additional function call overhead
///
/// ### Generated Code Example
///
/// For a complex VADL instruction:
/// ```vadl
/// instruction COMPLEX : MyFormat = forall i in 0..31 : X(rd + i) := X(rs1 + i) + X(rs2 + i)
/// ```
///
/// The `HelperCallGenerator` produces:
/// ```c
/// static bool trans_complex(DisasContext *ctx, arg_complex *a) {
///     TCGv_i32 rd_tmp = tcg_constant_i32(a->rd);
///     TCGv_i32 rs1_tmp = tcg_constant_i32(a->rs1);
///     TCGv_i32 rs2_tmp = tcg_constant_i32(a->rs2);
///     gen_helper_complex(tcg_env, rd_tmp, rs1_tmp, rs2_tmp);
///     return true;
/// }
/// ```
///
/// And a corresponding helper function is generated in `helper.c` that implements
/// the full instruction semantics.
///
/// @see DefaultGenerator
/// @see InstrInfo#asHelperCall()
///
@SuppressWarnings("unused")
class HelperCallGenerator {

  private Instruction insn;
  private CodeGeneratorAppendable builder;
  private String targetName;
  private InstrInfo info;
  private HelperParamPacking packing;

  HelperCallGenerator(Instruction instr,
                      IssConfiguration configuration) {
    this.insn = instr;
    this.info = instrInfo(instr);
    this.builder = new StringBuilderAppendable();
    this.targetName = configuration.targetName();
    this.packing = HelperParamPacking.from(info);
  }

  String fetch() {

    // static bool trans_<name>(DisasContext *ctx, arg_<name> *a) {\n
    builder.append("static bool trans_")
        .append(info.cIdentName())
        .append("(DisasContext *ctx, arg_")
        .append(info.cIdentName())
        .appendLn(" *a) {")
        .indent();

    builder.appendLn("trace_" + this.targetName.toLowerCase() + "_instr_trans(__func__);");

    genArgTcgVs();
    genPcUpdate();
    genHelperCall();
    genTbLookupIfNecessary();

    builder.appendLn("return true;")
        .unindent()
        .appendLn("}");
    return builder.toString();
  }

  private void genArgTcgVs() {
    IntStream.range(0, packing.blockCount())
        .forEach(blockIndex -> {
          var stmt = "TCGv_i64 packed" + blockIndex + "_tmp = tcg_constant_i64("
              + packedBlockExpr(blockIndex) + ");";
          builder.appendLn(stmt);
        });
  }

  private void genHelperCall() {
    var args = Stream.concat(Stream.of("tcg_env"),
        fieldArgs()).collect(Collectors.joining(", "));
    var call = "gen_helper_"
        + info.helperName()
        + "(" + args + ");";
    builder.appendLn(call);
  }

  /// If the instruction reads the PC, we must update the
  /// PC TCG variable; otherwise the helper function won't have
  /// the current PC in its environment.
  private void genPcUpdate() {
    if (doesPCRead()) {
      builder.appendLn("gen_update_pc_diff(ctx, 0);");
    }
  }

  /// If the instruction writes the PC, we must do a new TB lookup.
  private void genTbLookupIfNecessary() {
    if (doesPCUpdate()) {
      var instrWidth = insn.format().type().bitWidth() / 8;
      builder.appendLn("gen_update_pc_diff(ctx, " + instrWidth + ");");
      // we don't have to update the PC, as the helper already does this.
      builder.appendLn("tcg_gen_lookup_and_goto_ptr();")
          .appendLn("ctx->base.is_jmp = DISAS_NORETURN;");
    }
  }

  private boolean doesPCUpdate() {
    return insn.behavior().getNodes(WriteRegTensorNode.class)
        .anyMatch(WriteRegTensorNode::isPcAccess);
  }

  private boolean doesPCRead() {
    return insn.behavior().getNodes(ReadRegTensorNode.class)
        .anyMatch(ReadRegTensorNode::isPcAccess);
  }

  private Stream<String> fieldArgs() {
    return IntStream.range(0, packing.blockCount())
        .mapToObj(i -> "packed" + i + "_tmp");
  }

  private String paramName(ParamNode p) {
    return p.definition().simpleName();
  }

  private String packedBlockExpr(int blockIndex) {
    var slices = packing.slicesForBlock(blockIndex);
    if (slices.isEmpty()) {
      return "UINT64_C(0)";
    }
    return slices.stream().map(this::packedSliceExpr)
        .collect(Collectors.joining(" | ", "(", ")"));
  }

  private String packedSliceExpr(HelperParamPacking.ParamSlice slice) {
    var p = slice.param().param();
    var expr = "((uint64_t)(a->" + paramName(p) + "))";
    if (slice.param().bitWidth() < 64) {
      expr = "(" + expr + " & " + HelperParamPacking.u64MaskLiteral(slice.param().bitWidth()) + ")";
    }
    if (slice.paramOffset() > 0) {
      expr = "(" + expr + " >> " + slice.paramOffset() + ")";
    }
    if (slice.width() < 64) {
      expr = "(" + expr + " & " + HelperParamPacking.u64MaskLiteral(slice.width()) + ")";
    }
    if (slice.blockOffset() > 0) {
      expr = "(" + expr + " << " + slice.blockOffset() + ")";
    }
    return expr;
  }

}
