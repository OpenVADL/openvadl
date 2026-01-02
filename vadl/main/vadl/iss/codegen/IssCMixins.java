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

import static vadl.iss.IssUtils.internalError;

import vadl.cppCodeGen.context.CGenContext;
import vadl.iss.passes.nodes.IssConstExtractNode;
import vadl.iss.passes.nodes.IssGhostCastNode;
import vadl.iss.passes.nodes.IssMoveNode;
import vadl.iss.passes.nodes.IssSelectNode;
import vadl.iss.passes.nodes.IssTempExprNode;
import vadl.iss.passes.nodes.IssValExtractNode;
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.iss.passes.opDecomposition.nodes.IssMul2Node;
import vadl.iss.passes.opDecomposition.nodes.IssMulhNode;
import vadl.iss.passes.safeResourceRead.nodes.ExprSaveNode;
import vadl.iss.passes.tcgLowering.TcgExtend;
import vadl.iss.passes.tcgLowering.nodes.TcgAddNode;
import vadl.iss.passes.tcgLowering.nodes.TcgAndNode;
import vadl.iss.passes.tcgLowering.nodes.TcgBiNopNode;
import vadl.iss.passes.tcgLowering.nodes.TcgBr;
import vadl.iss.passes.tcgLowering.nodes.TcgBrCond;
import vadl.iss.passes.tcgLowering.nodes.TcgConstSelectNode;
import vadl.iss.passes.tcgLowering.nodes.TcgCountZerosNode;
import vadl.iss.passes.tcgLowering.nodes.TcgCtpopNode;
import vadl.iss.passes.tcgLowering.nodes.TcgDepositNode;
import vadl.iss.passes.tcgLowering.nodes.TcgDivNode;
import vadl.iss.passes.tcgLowering.nodes.TcgExtendNode;
import vadl.iss.passes.tcgLowering.nodes.TcgExtractNode;
import vadl.iss.passes.tcgLowering.nodes.TcgFreeTemp;
import vadl.iss.passes.tcgLowering.nodes.TcgGenException;
import vadl.iss.passes.tcgLowering.nodes.TcgGenLabel;
import vadl.iss.passes.tcgLowering.nodes.TcgGottoTb;
import vadl.iss.passes.tcgLowering.nodes.TcgHelperCall;
import vadl.iss.passes.tcgLowering.nodes.TcgLoadMemory;
import vadl.iss.passes.tcgLowering.nodes.TcgLookupAndGotoPtr;
import vadl.iss.passes.tcgLowering.nodes.TcgMovCondNode;
import vadl.iss.passes.tcgLowering.nodes.TcgMoveNode;
import vadl.iss.passes.tcgLowering.nodes.TcgMul2Node;
import vadl.iss.passes.tcgLowering.nodes.TcgMulNode;
import vadl.iss.passes.tcgLowering.nodes.TcgMulhNode;
import vadl.iss.passes.tcgLowering.nodes.TcgNegNode;
import vadl.iss.passes.tcgLowering.nodes.TcgNotNode;
import vadl.iss.passes.tcgLowering.nodes.TcgOrNode;
import vadl.iss.passes.tcgLowering.nodes.TcgRemNode;
import vadl.iss.passes.tcgLowering.nodes.TcgRotlNode;
import vadl.iss.passes.tcgLowering.nodes.TcgRotrNode;
import vadl.iss.passes.tcgLowering.nodes.TcgSarNode;
import vadl.iss.passes.tcgLowering.nodes.TcgSetCond;
import vadl.iss.passes.tcgLowering.nodes.TcgSetIsJmp;
import vadl.iss.passes.tcgLowering.nodes.TcgSetLabel;
import vadl.iss.passes.tcgLowering.nodes.TcgShlNode;
import vadl.iss.passes.tcgLowering.nodes.TcgShrNode;
import vadl.iss.passes.tcgLowering.nodes.TcgStoreMemory;
import vadl.iss.passes.tcgLowering.nodes.TcgSubNode;
import vadl.iss.passes.tcgLowering.nodes.TcgTruncateNode;
import vadl.iss.passes.tcgLowering.nodes.TcgUnaryNopNode;
import vadl.iss.passes.tcgLowering.nodes.TcgXorNode;
import vadl.javaannotations.Handler;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;

/**
 * The ISS C mixins for all ISS intermediate nodes added to behaviors.
 * Most of those nodes are replaced before code generation and therefore
 * crash by default if they are getting emitted.
 */
public interface IssCMixins {

  /**
   * Bundles all valid ISS node mixins.
   */
  interface Default extends IssExtract, IssExpr {
  }

  /**
   * ISS specific expressions (subtypes of
   * {@link vadl.iss.passes.opDecomposition.nodes.IssExprNode}).
   */
  interface IssExpr {
    @Handler
    @SuppressWarnings("MissingJavadocMethod")
    default void handle(CGenContext<Node> ctx, IssValExtractNode node) {
      var valW = node.value().type().asDataType().bitWidth();
      var ofsW = node.ofs().type().asDataType().bitWidth();
      var lenW = node.len().type().asDataType().bitWidth();
      // we perform a shift >> to clear the offset.
      // then we extract the result using (s/u)extract.

      var extract = node.extendMode() == TcgExtend.ZERO ? "VADL_uextract" : "VADL_sextract";

      ctx.wr(extract + "( ");
      // inner shift of value
      ctx.wr("VADL_lsr(").gen(node.value())
          .wr(", %s, ", valW).gen(node.value()).wr(", %s)", ofsW);

      ctx.wr(", %s )", lenW);
    }

    @Handler
    default void handle(CGenContext<Node> ctx, IssGhostCastNode toHandle) {
      // just emit the inner value
      ctx.gen(toHandle.value());
    }

    @Handler
    @SuppressWarnings("MissingJavadocMethod")
    default void handle(CGenContext<Node> ctx, IssSelectNode toHandle) {
      // IssSelectNodes are always turned into TCG nodes (TcgConstSelect or TcgMovCond).
      // If the original SelectNode was not scheduled, it got not converted to an IssSelectNode.
      throw new IllegalStateException("The IssSelectNode should never be generated as C code.");
    }

    @Handler
    default void handle(CGenContext<Node> ctx, IssMoveNode toHandle) {
      // should be replaced by a TCG move
      throw new IllegalStateException("The IssMoveExprNode should never be generated as C code.");
    }

    @Handler
    default void handle(CGenContext<Node> ctx, IssTempExprNode toHandle) {
      // does nothing
    }

    @Handler
    @SuppressWarnings("MissingJavadocMethod")
    default void handle(CGenContext<Node> ctx, ExprSaveNode toHandle) {
      internalError(toHandle,
          "[IssCMixins] The node %s is not handled to be generated as a C function.", toHandle);
    }

    @Handler
    @SuppressWarnings("MissingJavadocMethod")
    default void handle(CGenContext<Node> ctx, IssMulhNode toHandle) {
      var aw = toHandle.arg1().type().asDataType().bitWidth();
      var bw = toHandle.arg2().type().asDataType().bitWidth();

      String fn;
      switch (toHandle.kind()) {
        case SIGNED_SIGNED -> fn = "VADL_smulh";
        case UNSIGNED_UNSIGNED -> fn = "VADL_umulh";
        case SIGNED_UNSIGNED -> fn = "VADL_sumulh";
        default -> throw new IllegalStateException("Unknown IssMulKind: " + toHandle.kind());
      }

      ctx.wr(fn + "(")
          .gen(toHandle.arg1())
          .wr(", %s, ", aw)
          .gen(toHandle.arg2())
          .wr(", %s)", bw);
    }

    @Handler
    default void handle(CGenContext<Node> ctx, IssMul2Node toHandle) {
      internalError(toHandle,
          "[IssCMixins] The node %s is not handled to be generated as a C function.", toHandle);
    }
  }

  /**
   * The ISS extract node rendering.
   */
  interface IssExtract {


    /**
     * Implements the C code representation of the {@link IssConstExtractNode}.
     */
    @Handler
    default void handle(CGenContext<Node> ctx,
                        IssConstExtractNode node) {
      var sign = node.isSigned() ? "s" : "u";
      ctx.wr("VADL_" + sign + "extract(")
          .gen(node.value())
          .wr("," + node.fromWidth())
          .wr(")");
    }

  }

  /**
   * Implements the register write in the {@code cpu.c}.
   */
  interface CpuSourceWriteRegTensor {

    @Handler
    @SuppressWarnings("MissingJavadocMethod")
    default void handle(CGenContext<Node> ctx,
                        WriteRegTensorNode node) {
      var reg = node.regTensor();
      ctx.wr("set_cpu_" + reg.simpleName().toLowerCase() + "(env");
      for (var i : node.indices()) {
        ctx.wr(", ").gen(i);
      }
      ctx.wr(", ").gen(node.value()).wr(")");
    }

    @Handler
    @SuppressWarnings("MissingJavadocMethod")
    default void handle(CGenContext<Node> ctx,
                        ReadRegTensorNode node) {
      var reg = node.regTensor();
      ctx.wr("get_cpu_" + reg.simpleName().toLowerCase() + "(env");
      for (var i : node.indices()) {
        ctx.wr(", ").gen(i);
      }
      ctx.wr(")");
    }
  }

  /**
   * Implements the memory read/write in the {@code cpu.c} and all non-tcg behavior.
   */
  interface CpuSourceReadWriteMemory {

    @Handler
    @SuppressWarnings("MissingJavadocMethod")
    default void handle(CGenContext<Node> ctx, WriteMemNode node) {
      handle(ctx, node, true);
    }

    @Handler
    @SuppressWarnings("MissingJavadocMethod")
    default void handle(CGenContext<Node> ctx, ReadMemNode node) {
      handle(ctx, node, true);
    }


    /// In helper functions IO operations require the return address for unwinding.
    /// This can be obtained by calling GETPC().
    /// However, this is only available in TCG helpers.
    @SuppressWarnings("MissingJavadocMethod")
    static void handle(CGenContext<Node> ctx, ReadMemNode node, boolean withGetPcRetAddr) {
      var bitWidth = node.readBitWidth();
      var suffix = switch (bitWidth) {
        case 8 -> "ub";
        case 16 -> "uw";
        case 32 -> "l";
        case 64 -> "q";
        default -> throw new IllegalArgumentException(
            "Unsupported memory read width: " + bitWidth);
      };
      var ra = withGetPcRetAddr ? "GETPC()" : "0";
      ctx.wr("cpu_ld" + suffix + "_data_ra(env, ");
      ctx.gen(node.address()).wr(", ").wr(ra).wr(")");
    }

    /// In helper functions IO operations require the return address for unwinding.
    /// This can be obtained by calling GETPC().
    /// However, this is only available in TCG helpers.
    @SuppressWarnings("MissingJavadocMethod")
    static void handle(CGenContext<Node> ctx, WriteMemNode node, boolean withGetPcRetAddr) {
      var bitWidth = node.writeBitWidth();
      var suffix = switch (bitWidth) {
        case 8 -> "b";
        case 16 -> "w";
        case 32 -> "l";
        case 64 -> "q";
        default -> throw new IllegalArgumentException(
            "Unsupported memory write width: " + bitWidth);
      };
      var ra = withGetPcRetAddr ? "GETPC()" : "0";
      ctx.wr("cpu_st" + suffix + "_data_ra(env, ");
      ctx.gen(node.address()).wr(", ");
      ctx.gen(node.value()).wr(",").wr(ra).wr(")");
    }
  }

  /**
   * All TCG nodes, so this can be used as mixins if a generator has to
   * also implement iss nodes for C behavior rendering.
   */
  interface InvalidTcgC {


    @Handler
    default void handle(CGenContext<Node> ctx, TcgRotlNode toHandle) {
      internalError(toHandle,
          "[IssCMixins] The node %s is not handled to be generated as a C function.", toHandle);
    }

    @Handler
    default void handle(CGenContext<Node> ctx, TcgLoadMemory toHandle) {
      internalError(toHandle,
          "[IssCMixins] The node %s is not handled to be generated as a C function.", toHandle);
    }

    @Handler
    default void handle(CGenContext<Node> ctx, TcgCountZerosNode toHandle) {
      internalError(toHandle,
          "[IssCMixins] The node %s is not handled to be generated as a C function.", toHandle);
    }

    @Handler
    default void handle(CGenContext<Node> ctx, TcgTruncateNode toHandle) {
      internalError(toHandle,
          "[IssCMixins] The node %s is not handled to be generated as a C function.", toHandle);
    }

    @Handler
    default void handle(CGenContext<Node> ctx, TcgDivNode toHandle) {
      internalError(toHandle,
          "[IssCMixins] The node %s is not handled to be generated as a C function.", toHandle);
    }

    @Handler
    default void handle(CGenContext<Node> ctx, TcgOrNode toHandle) {
      internalError(toHandle,
          "[IssCMixins] The node %s is not handled to be generated as a C function.", toHandle);
    }

    @Handler
    default void handle(CGenContext<Node> ctx, TcgFreeTemp toHandle) {
      internalError(toHandle,
          "[IssCMixins] The node %s is not handled to be generated as a C function.", toHandle);
    }

    @Handler
    default void handle(CGenContext<Node> ctx, TcgMulNode toHandle) {
      internalError(toHandle,
          "[IssCMixins] The node %s is not handled to be generated as a C function.", toHandle);
    }

    @Handler
    default void handle(CGenContext<Node> ctx, TcgShlNode toHandle) {
      internalError(toHandle,
          "[IssCMixins] The node %s is not handled to be generated as a C function.", toHandle);
    }

    @Handler
    default void handle(CGenContext<Node> ctx, TcgSubNode toHandle) {
      internalError(toHandle,
          "[IssCMixins] The node %s is not handled to be generated as a C function.", toHandle);
    }

    @Handler
    default void handle(CGenContext<Node> ctx, TcgMovCondNode toHandle) {
      internalError(toHandle,
          "[IssCMixins] The node %s is not handled to be generated as a C function.", toHandle);
    }

    @Handler
    default void handle(CGenContext<Node> ctx, TcgVRefNode toHandle) {
      internalError(toHandle,
          "[IssCMixins] The node %s is not handled to be generated as a C function.", toHandle);
    }

    @Handler
    default void handle(CGenContext<Node> ctx, TcgXorNode toHandle) {
      internalError(toHandle,
          "[IssCMixins] The node %s is not handled to be generated as a C function.", toHandle);
    }

    @Handler
    default void handle(CGenContext<Node> ctx, TcgBiNopNode toHandle) {
      internalError(toHandle,
          "[IssCMixins] The node %s is not handled to be generated as a C function.", toHandle);
    }

    @Handler
    default void handle(CGenContext<Node> ctx, TcgRemNode toHandle) {
      internalError(toHandle,
          "[IssCMixins] The node %s is not handled to be generated as a C function.", toHandle);
    }

    @Handler
    default void handle(CGenContext<Node> ctx, TcgGenLabel toHandle) {
      internalError(toHandle,
          "[IssCMixins] The node %s is not handled to be generated as a C function.", toHandle);
    }

    @Handler
    default void handle(CGenContext<Node> ctx, TcgSetIsJmp toHandle) {
      internalError(toHandle,
          "[IssCMixins] The node %s is not handled to be generated as a C function.", toHandle);
    }

    @Handler
    default void handle(CGenContext<Node> ctx, TcgGottoTb toHandle) {
      internalError(toHandle,
          "[IssCMixins] The node %s is not handled to be generated as a C function.", toHandle);
    }

    @Handler
    default void handle(CGenContext<Node> ctx, TcgMulhNode toHandle) {
      internalError(toHandle,
          "[IssCMixins] The node %s is not handled to be generated as a C function.", toHandle);
    }

    @Handler
    default void handle(CGenContext<Node> ctx, TcgSarNode toHandle) {
      internalError(toHandle,
          "[IssCMixins] The node %s is not handled to be generated as a C function.", toHandle);
    }

    @Handler
    default void handle(CGenContext<Node> ctx, TcgSetCond toHandle) {
      internalError(toHandle,
          "[IssCMixins] The node %s is not handled to be generated as a C function.", toHandle);
    }

    @Handler
    default void handle(CGenContext<Node> ctx, TcgCtpopNode toHandle) {
      internalError(toHandle,
          "[IssCMixins] The node %s is not handled to be generated as a C function.", toHandle);
    }

    @Handler
    default void handle(CGenContext<Node> ctx, TcgSetLabel toHandle) {
      internalError(toHandle,
          "[IssCMixins] The node %s is not handled to be generated as a C function.", toHandle);
    }

    @Handler
    default void handle(CGenContext<Node> ctx, TcgAddNode toHandle) {
      internalError(toHandle,
          "[IssCMixins] The node %s is not handled to be generated as a C function.", toHandle);
    }

    @Handler
    default void handle(CGenContext<Node> ctx, TcgStoreMemory toHandle) {
      internalError(toHandle,
          "[IssCMixins] The node %s is not handled to be generated as a C function.", toHandle);
    }

    @Handler
    default void handle(CGenContext<Node> ctx, TcgMoveNode toHandle) {
      internalError(toHandle,
          "[IssCMixins] The node %s is not handled to be generated as a C function.", toHandle);
    }

    @Handler
    default void handle(CGenContext<Node> ctx, TcgNotNode toHandle) {
      internalError(toHandle,
          "[IssCMixins] The node %s is not handled to be generated as a C function.", toHandle);
    }

    @Handler
    default void handle(CGenContext<Node> ctx, TcgNegNode toHandle) {
      internalError(toHandle,
          "[IssCMixins] The node %s is not handled to be generated as a C function.", toHandle);
    }

    @Handler
    default void handle(CGenContext<Node> ctx, TcgLookupAndGotoPtr toHandle) {
      internalError(toHandle,
          "[IssCMixins] The node %s is not handled to be generated as a C function.", toHandle);
    }

    @Handler
    default void handle(CGenContext<Node> ctx, TcgExtendNode toHandle) {
      internalError(toHandle,
          "[IssCMixins] The node %s is not handled to be generated as a C function.", toHandle);
    }

    @Handler
    default void handle(CGenContext<Node> ctx, TcgBr toHandle) {
      internalError(toHandle,
          "[IssCMixins] The node %s is not handled to be generated as a C function.", toHandle);
    }

    @Handler
    default void handle(CGenContext<Node> ctx, TcgBrCond toHandle) {
      internalError(toHandle,
          "[IssCMixins] The node %s is not handled to be generated as a C function.", toHandle);
    }

    @Handler
    default void handle(CGenContext<Node> ctx, TcgExtractNode toHandle) {
      internalError(toHandle,
          "[IssCMixins] The node %s is not handled to be generated as a C function.", toHandle);
    }

    @Handler
    default void handle(CGenContext<Node> ctx, TcgRotrNode toHandle) {
      internalError(toHandle,
          "[IssCMixins] The node %s is not handled to be generated as a C function.", toHandle);
    }

    @Handler
    default void handle(CGenContext<Node> ctx, TcgHelperCall toHandle) {
      internalError(toHandle,
          "[IssCMixins] The node %s is not handled to be generated as a C function.", toHandle);
    }

    @Handler
    default void handle(CGenContext<Node> ctx, TcgShrNode toHandle) {
      internalError(toHandle,
          "[IssCMixins] The node %s is not handled to be generated as a C function.", toHandle);
    }

    @Handler
    default void handle(CGenContext<Node> ctx, TcgMul2Node toHandle) {
      internalError(toHandle,
          "[IssCMixins] The node %s is not handled to be generated as a C function.", toHandle);
    }

    @Handler
    default void handle(CGenContext<Node> ctx, TcgConstSelectNode toHandle) {
      internalError(toHandle,
          "[IssCMixins] The node %s is not handled to be generated as a C function.", toHandle);
    }

    @Handler
    default void handle(CGenContext<Node> ctx, TcgGenException toHandle) {
      internalError(toHandle,
          "[IssCMixins] The node %s is not handled to be generated as a C function.", toHandle);
    }

    @Handler
    default void handle(CGenContext<Node> ctx, TcgUnaryNopNode toHandle) {
      internalError(toHandle,
          "[IssCMixins] The node %s is not handled to be generated as a C function.", toHandle);
    }

    @Handler
    default void handle(CGenContext<Node> ctx, TcgDepositNode toHandle) {
      internalError(toHandle,
          "[IssCMixins] The node %s is not handled to be generated as a C function.", toHandle);
    }

    @Handler
    default void handle(CGenContext<Node> ctx, TcgAndNode toHandle) {
      internalError(toHandle,
          "[IssCMixins] The node %s is not handled to be generated as a C function.", toHandle);
    }
  }

}
