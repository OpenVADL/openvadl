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

import vadl.cppCodeGen.context.CGenContext;
import vadl.iss.passes.nodes.IssConstExtractNode;
import vadl.iss.passes.nodes.IssGhostCastNode;
import vadl.iss.passes.nodes.IssMoveNode;
import vadl.iss.passes.nodes.IssSelectNode;
import vadl.iss.passes.nodes.IssTempExprNode;
import vadl.iss.passes.nodes.IssValExtractNode;
import vadl.iss.passes.opDecomposition.nodes.IssMul2Node;
import vadl.iss.passes.opDecomposition.nodes.IssMulhNode;
import vadl.iss.passes.safeResourceRead.nodes.ExprSaveNode;
import vadl.iss.passes.tcgLowering.TcgExtend;
import vadl.javaannotations.Handler;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ReadMemNode;
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
    default void handle(CGenContext<Node> ctx, ExprSaveNode toHandle) {
      throw new UnsupportedOperationException("Type ExprSaveNode not yet implemented");
    }

    @Handler
    default void handle(CGenContext<Node> ctx, IssMulhNode toHandle) {
      throw new UnsupportedOperationException("Type IssMulhNode not yet implemented");
    }

    @Handler
    default void handle(CGenContext<Node> ctx, IssMul2Node toHandle) {
      throw new UnsupportedOperationException("Type IssMul2Node not yet implemented");
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
  }

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

}
