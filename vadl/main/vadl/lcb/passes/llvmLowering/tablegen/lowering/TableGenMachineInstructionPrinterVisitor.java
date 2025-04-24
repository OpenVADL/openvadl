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

package vadl.lcb.passes.llvmLowering.tablegen.lowering;

import java.io.StringWriter;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionNode;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionParameterNode;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionValueNode;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbPseudoInstructionNode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBasicBlockSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmFieldAccessRefNode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmReadRegFileNode;
import vadl.lcb.passes.llvmLowering.strategies.LlvmInstructionLoweringStrategy;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenMachineInstructionVisitor;
import vadl.viam.Constant;
import vadl.viam.graph.NodeList;
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
import vadl.viam.graph.dependency.WriteRegTensorNode;
import vadl.viam.graph.dependency.ZeroExtendNode;

/**
 * Visitor for machine instructions.
 */
public class TableGenMachineInstructionPrinterVisitor implements TableGenMachineInstructionVisitor {
  protected final StringWriter writer = new StringWriter();

  public String getResult() {
    return writer.toString();
  }

  protected void joinArgumentsWithComma(NodeList<ExpressionNode> args) {
    for (int i = 0; i < args.size(); i++) {
      visit(args.get(i));

      if (i < args.size() - 1) {
        writer.write(", ");
      }
    }
  }

  @Override
  public void visit(LcbPseudoInstructionNode node) {
    writer.write("(");
    writer.write(node.instruction().identifier.simpleName() + " ");

    joinArgumentsWithComma(node.arguments());

    writer.write(")");
  }

  @Override
  public void visit(LcbMachineInstructionNode node) {
    writer.write("(");
    writer.write(node.outputInstructionName().value() + " ");

    joinArgumentsWithComma(node.arguments());

    writer.write(")");
  }

  @Override
  public void visit(LcbMachineInstructionParameterNode machineInstructionParameterNode) {
    writer.write(machineInstructionParameterNode.instructionOperand().render());
  }

  @Override
  public void visit(LcbMachineInstructionValueNode machineInstructionValueNode) {
    writer.write("(" + machineInstructionValueNode.valueType().getLlvmType() + " "
        + machineInstructionValueNode.constant().asVal().intValue() + ")");
  }

  @Override
  public void visit(LlvmBasicBlockSD basicBlockSD) {
    writer.write(basicBlockSD.parameter().render());
  }

  @Override
  public void visit(LlvmFieldAccessRefNode fieldAccessRefNode) {
    var operand = LlvmInstructionLoweringStrategy.generateTableGenInputOutput(fieldAccessRefNode);
    writer.write(operand.render());
  }

  @Override
  public void visit(LlvmReadRegFileNode llvmReadRegFileNode) {
    var operand = LlvmInstructionLoweringStrategy.generateTableGenInputOutput(llvmReadRegFileNode);
    writer.write(operand.render());
  }

  @Override
  public void visit(ConstantNode node) {
    if (node.constant() instanceof Constant.Str str) {
      writer.write(str.value());
    } else if (node.constant() instanceof Constant.Value v) {
      writer.write(v.intValue());
    } else {
      throw new RuntimeException("not implemented");
    }
  }

  @Override
  public void visit(BuiltInCall node) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(WriteRegTensorNode writeRegNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(WriteMemNode writeMemNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(SliceNode sliceNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(SelectNode selectNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(ReadRegTensorNode node) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(ReadMemNode readMemNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(LetNode letNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(FuncParamNode funcParamNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(FuncCallNode funcCallNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(FieldRefNode fieldRefNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(FieldAccessRefNode fieldAccessRefNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(AbstractBeginNode abstractBeginNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(InstrEndNode instrEndNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(ReturnNode returnNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(BranchEndNode branchEndNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(InstrCallNode instrCallNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(IfNode ifNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(ZeroExtendNode node) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(SignExtendNode node) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(TruncateNode node) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(ExpressionNode expressionNode) {
    expressionNode.accept(this);
  }

  @Override
  public void visit(SideEffectNode sideEffectNode) {
    sideEffectNode.accept(this);
  }
}
