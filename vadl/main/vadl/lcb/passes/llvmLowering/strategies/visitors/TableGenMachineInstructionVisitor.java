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

package vadl.lcb.passes.llvmLowering.strategies.visitors;

import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionNode;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionParameterNode;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionValueNode;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbPseudoInstructionNode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBasicBlockSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmFieldAccessRefNode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmReadRegFileNode;
import vadl.viam.graph.Graph;
import vadl.viam.graph.GraphNodeVisitor;

/**
 * Visitor for machine instruction's {@link Graph}.
 */
public interface TableGenMachineInstructionVisitor extends GraphNodeVisitor {
  /**
   * Visit {@link LcbPseudoInstructionNode}.
   */
  void visit(LcbPseudoInstructionNode pseudoInstructionNode);

  /**
   * Visit {@link LcbMachineInstructionNode}.
   */
  void visit(LcbMachineInstructionNode machineInstructionNode);

  /**
   * Visit {@link LcbMachineInstructionParameterNode}.
   */
  void visit(LcbMachineInstructionParameterNode machineInstructionParameterNode);

  /**
   * Visit {@link LcbMachineInstructionValueNode}.
   */
  void visit(LcbMachineInstructionValueNode machineInstructionValueNode);

  /**
   * Visit {@link LlvmBasicBlockSD}.
   */
  void visit(LlvmBasicBlockSD basicBlockSD);

  /**
   * Visit {@link LlvmFieldAccessRefNode}.
   */
  void visit(LlvmFieldAccessRefNode fieldAccessRefNode);

  /**
   * Visit {@link LlvmReadRegFileNode}.
   */
  void visit(LlvmReadRegFileNode llvmReadRegFileNode);
}
