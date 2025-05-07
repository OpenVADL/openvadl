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
import vadl.iss.passes.nodes.IssValExtractNode;
import vadl.iss.passes.opDecomposition.nodes.IssMul2Node;
import vadl.iss.passes.opDecomposition.nodes.IssMulhNode;
import vadl.iss.passes.safeResourceRead.nodes.ExprSaveNode;
import vadl.iss.passes.tcgLowering.TcgExtend;
import vadl.javaannotations.Handler;
import vadl.viam.graph.Node;
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

  interface IssExpr {
    @Handler
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
      ctx.wr("set_" + reg.simpleName().toLowerCase() + "(");
      for (var i : node.indices()) {
        ctx.gen(i).wr(",");
      }
      ctx.gen(node.value()).wr(")");
    }
  }

}
