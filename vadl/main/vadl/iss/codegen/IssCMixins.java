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

import vadl.cppCodeGen.context.CGenContext;
import vadl.iss.passes.nodes.IssConstExtractNode;
import vadl.javaannotations.Handler;
import vadl.viam.graph.Node;

/**
 * The ISS C mixins for all ISS intermediate nodes added to behaviors.
 * Most of those nodes are replaced before code generation and therefore
 * crash by default if they are getting emitted.
 */
public interface IssCMixins {

  /**
   * Bundles all Invalid ISS node mixins.
   */
  interface Invalid extends IssExpr {

  }

  /**
   * Bundles all valid ISS node mixins.
   */
  interface Default extends IssExtract {
  }

  /**
   * The invalid ISS Expr Node mixin.
   */
  interface IssExpr {

    @Handler
    default void impl(CGenContext<Node> ctx,
                      vadl.iss.passes.opDecomposition.nodes.IssExprNode node) {
      throwNotAllowed(node, "IssExprNode");
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
    default void impl(CGenContext<Node> ctx,
                      IssConstExtractNode node) {
      var sign = node.isSigned() ? "s" : "u";
      ctx.wr("VADL_" + sign + "extract(")
          .gen(node.value())
          .wr("," + node.fromWidth())
          .wr(")");
    }

  }

}
