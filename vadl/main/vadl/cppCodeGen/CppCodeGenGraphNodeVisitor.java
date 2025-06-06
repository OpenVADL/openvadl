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

package vadl.cppCodeGen;

import vadl.cppCodeGen.model.nodes.CppUpdateBitRangeNode;
import vadl.cppCodeGen.passes.typeNormalization.CppSignExtendNode;
import vadl.cppCodeGen.passes.typeNormalization.CppTruncateNode;
import vadl.cppCodeGen.passes.typeNormalization.CppZeroExtendNode;
import vadl.viam.graph.GraphNodeVisitor;

/**
 * Visitor for nodes of the cpp codegen layer.
 */
public interface CppCodeGenGraphNodeVisitor extends GraphNodeVisitor {
  /**
   * Visit {@link CppSignExtendNode}.
   */
  void visit(CppSignExtendNode node);

  /**
   * Visit {@link CppZeroExtendNode}.
   */
  void visit(CppZeroExtendNode node);

  /**
   * Visit {@link CppTruncateNode}.
   */
  void visit(CppTruncateNode node);

  /**
   * Visit {@link CppUpdateBitRangeNode}.
   */
  void visit(CppUpdateBitRangeNode node);
}
