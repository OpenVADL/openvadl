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

package vadl.viam.graph;

import vadl.viam.graph.control.AbstractBeginNode;
import vadl.viam.graph.control.BranchEndNode;
import vadl.viam.graph.control.IfNode;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.graph.control.InstrEndNode;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncCallNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.LetNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.SelectNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;
import vadl.viam.graph.dependency.ZeroExtendNode;

/**
 * Interface for visiting multiple {@link Node} and its subtypes.
 *
 * <p>Note that this visitor should only be used if traditional node search/graph manipulation
 * is not sufficient. Most of the times it is far easier to implement a pass without visitors.</p>
 *
 * @deprecated Using visitors for nodes is deprecated. To handle many different types of nodes
 *     use {@link vadl.javaannotations.DispatchFor @DispatchFor} instead.
 */
@Deprecated
public interface GraphNodeVisitor {
  /**
   * Catchall method when overloading did not work.
   */
  default void visit(Node node) {
    throw new RuntimeException("Node type is not implemented: " + node.getClass());
  }

  /**
   * Visit {@link ConstantNode}.
   */
  void visit(ConstantNode node);

  /**
   * Visit {@link BuiltInCall}.
   */
  void visit(BuiltInCall node);

  /**
   * Visit {@link WriteRegNode}.
   */
  void visit(WriteRegTensorNode node);

  /**
   * Visit {@link WriteMemNode}.
   */
  void visit(WriteMemNode writeMemNode);

  /**
   * Visit {@link SliceNode}.
   */
  void visit(SliceNode sliceNode);

  /**
   * Visit {@link SelectNode}.
   */
  void visit(SelectNode selectNode);

  /**
   * Visit {@link ReadRegTensorNode}.
   */
  void visit(ReadRegTensorNode node);

  /**
   * Visit {@link ReadMemNode}.
   */
  void visit(ReadMemNode readMemNode);

  /**
   * Visit {@link LetNode}.
   */
  void visit(LetNode letNode);

  /**
   * Visit {@link FuncParamNode}.
   */
  void visit(FuncParamNode funcParamNode);

  /**
   * Visit {@link FuncCallNode}.
   */
  void visit(FuncCallNode funcCallNode);

  /**
   * Visit {@link FieldRefNode}.
   */
  void visit(FieldRefNode fieldRefNode);

  /**
   * Visit {@link FieldAccessRefNode}.
   */
  void visit(FieldAccessRefNode fieldAccessRefNode);

  /**
   * Visit {@link AbstractBeginNode}.
   */
  void visit(AbstractBeginNode abstractBeginNode);

  /**
   * Visit {@link InstrEndNode}.
   */
  void visit(InstrEndNode instrEndNode);

  /**
   * Visit {@link ReturnNode}.
   */
  void visit(ReturnNode returnNode);

  /**
   * Visit {@link BranchEndNode}.
   */
  void visit(BranchEndNode branchEndNode);

  /**
   * Visit {@link InstrCallNode}.
   */
  void visit(InstrCallNode instrCallNode);

  /**
   * Visit {@link IfNode}.
   */
  void visit(IfNode ifNode);

  /**
   * Visit {@link ZeroExtendNode}.
   */
  void visit(ZeroExtendNode node);

  /**
   * Visit {@link SignExtendNode}.
   */
  void visit(SignExtendNode node);

  /**
   * Visit {@link TruncateNode}.
   */
  void visit(TruncateNode node);

  /**
   * Visit {@link ExpressionNode}.
   */
  void visit(ExpressionNode expressionNode);

  /**
   * Visit {@link SideEffectNode}.
   */
  void visit(SideEffectNode sideEffectNode);
}