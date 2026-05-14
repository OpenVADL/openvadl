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

import static vadl.iss.passes.TcgPassUtils.instrInfo;

import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.extensions.InstrInfo;
import vadl.utils.codegen.CodeGeneratorAppendable;
import vadl.utils.codegen.StringBuilderAppendable;
import vadl.viam.Instruction;
import vadl.viam.graph.dependency.ParamNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;

/**
 * Emits helper-call translation wrappers.
 *
 * <p>This generator renders a small translate function that packs instruction-format parameters
 * into helper arguments and delegates execution to the generated {@code gen_helper_<instr>} call.
 * It is used when the selected execution strategy is `HELPER_CALL`, and it also serves as the
 * temporary emission path reused by vector strategies whose dedicated translate-side emitters are
 * not implemented yet.</p>
 *
 * <p>The helper path is the general correctness fallback: it can represent instruction bodies that
 * are too complex for direct TCG emission, including vector instructions before their dedicated
 * planned emitters exist.</p>
 */
@SuppressWarnings("unused")
class HelperCallTranslateGenerator implements InstructionTranslateGenerator {

  private final Instruction insn;
  private final CodeGeneratorAppendable builder;
  private final String targetName;
  private final InstrInfo info;
  private final HelperParamPacking packing;

  HelperCallTranslateGenerator(Instruction instr,
                               IssConfiguration configuration) {
    this.insn = instr;
    this.info = instrInfo(instr);
    this.builder = new StringBuilderAppendable();
    this.targetName = configuration.targetName();
    this.packing = HelperParamPacking.from(info);
  }

  @Override
  public String fetch() {
    builder.append("static bool trans_")
        .append(info.cIdentName())
        .append("(DisasContext *ctx, arg_")
        .append(info.cIdentName())
        .appendLn(" *a) {")
        .indent();

    builder.appendLn("trace_" + targetName.toLowerCase() + "_instr_trans(__func__);");

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

  private void genPcUpdate() {
    if (doesPCRead()) {
      builder.appendLn("gen_update_pc_diff(ctx, 0);");
    }
  }

  private void genTbLookupIfNecessary() {
    if (doesPCUpdate()) {
      var instrWidth = insn.format().type().bitWidth() / 8;
      builder.appendLn("gen_update_pc_diff(ctx, " + instrWidth + ");");
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
