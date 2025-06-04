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
import java.util.Objects;
import vadl.error.Diagnostic;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.llvmLowering.LlvmNodeLowerable;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBasicBlockSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBrCcSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBrCondSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBrSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBrindSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmExtLoad;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmFieldAccessRefNode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmFrameIndexSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmLoadSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmReadRegFileNode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmSExtLoad;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmSetccSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmStoreSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmTargetCallSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmTruncStore;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmTypeCastSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmZExtLoad;
import vadl.lcb.passes.llvmLowering.strategies.LlvmInstructionLoweringStrategy;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenNodeVisitor;
import vadl.viam.Constant;
import vadl.viam.graph.Node;
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
import vadl.viam.graph.dependency.TupleGetFieldNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;
import vadl.viam.graph.dependency.ZeroExtendNode;

/**
 * Visitor for generating TableGen patterns.
 * The goal is to generate S-expressions with the same the
 * variable naming.
 */
public class TableGenPatternPrinterVisitor
    implements TableGenNodeVisitor {
  protected final StringWriter writer = new StringWriter();

  public String getResult() {
    return writer.toString();
  }

  @Override
  public void visit(Node node) {
    node.accept(this);
  }

  @Override
  public void visit(ConstantNode node) {
    node.ensure(node.constant() instanceof Constant.Value
        || node.constant() instanceof Constant.Str, "constant must be value or string");
    if (node.constant() instanceof Constant.Value constant) {
      var ty = ValueType.from(constant.type());
      if (ty.isPresent()) {
        writer.write(String.format("(%s %d)",
            ty.get().getLlvmType(),
            constant.intValue()));
      } else {
        throw Diagnostic.error(String.format("Constant has no valid LLVM type: '%s'.",
            node.constant().type().toString()), node.location()).build();
      }
    } else if (node.constant() instanceof Constant.Str str) {
      writer.write(str.value());
    }
  }

  @Override
  public void visit(BuiltInCall node) {
    writer.write("(");
    if (node instanceof LlvmNodeLowerable lowerable) {
      writer.write(lowerable.lower() + " ");
    } else {
      writer.write(node.builtIn().operator() + " ");
    }

    joinArgumentsWithComma(node.arguments());

    writer.write(")");
  }

  @Override
  public void visit(WriteRegTensorNode node) {

  }

  @Override
  public void visit(WriteMemNode writeMemNode) {

  }

  @Override
  public void visit(SliceNode sliceNode) {

  }

  @Override
  public void visit(SelectNode selectNode) {

  }

  @Override
  public void visit(ReadRegTensorNode readRegNode) {
    if (readRegNode.regTensor().isSingleRegister()) {
      writer.write(readRegNode.regTensor().identifier.simpleName());
    } else {
      throw new RuntimeException("not implemented");
    }
  }

  @Override
  public void visit(LlvmReadRegFileNode readRegFileNode) {
    var operand = LlvmInstructionLoweringStrategy.generateTableGenInputOutput(readRegFileNode);
    writer.write(operand.render());
  }

  @Override
  public void visit(LlvmFrameIndexSD node) {
    var operand = LlvmInstructionLoweringStrategy.generateTableGenInputOutput(node);
    writer.write(operand.render());
  }

  @Override
  public void visit(ReadMemNode readMemNode) {

  }

  @Override
  public void visit(LetNode letNode) {

  }

  @Override
  public void visit(FuncParamNode funcParamNode) {

  }

  @Override
  public void visit(FuncCallNode funcCallNode) {

  }

  @Override
  public void visit(FieldRefNode fieldRefNode) {

  }


  @Override
  public void visit(LlvmFieldAccessRefNode fieldAccessRefNode) {
    var operand = LlvmInstructionLoweringStrategy.generateTableGenInputOutput(fieldAccessRefNode);
    writer.write(operand.render());
  }

  @Override
  public void visit(LlvmBrCondSD node) {
    writer.write("(");
    writer.write(node.lower() + " ");
    visit(node.condition());
    writer.write(", ");
    visit(node.immOffset());
    writer.write(")");
  }

  @Override
  public void visit(LlvmTypeCastSD node) {
    writer.write("(" + node.lower() + " ");
    visit(node.value());
    writer.write(")");
  }

  @Override
  public void visit(LlvmTruncStore node) {
    writer.write("(" + node.lower() + " ");
    visit(node.value());
    writer.write(", ");
    visit(Objects.requireNonNull(node.address()));
    writer.write(")");
  }

  @Override
  public void visit(LlvmStoreSD node) {
    writer.write("(" + node.lower() + " ");
    visit(node.value());
    writer.write(", ");
    visit(Objects.requireNonNull(node.address()));
    writer.write(")");
  }

  @Override
  public void visit(LlvmLoadSD node) {
    writer.write("(" + node.lower() + " ");
    visit(Objects.requireNonNull(node.address()));
    writer.write(")");
  }

  @Override
  public void visit(LlvmSExtLoad node) {
    writer.write("(" + node.lower() + " ");
    visit(Objects.requireNonNull(node.address()));
    writer.write(")");
  }

  @Override
  public void visit(LlvmZExtLoad node) {
    writer.write("(" + node.lower() + " ");
    visit(Objects.requireNonNull(node.address()));
    writer.write(")");
  }

  @Override
  public void visit(LlvmExtLoad node) {
    writer.write("(" + node.lower() + " ");
    visit(Objects.requireNonNull(node.address()));
    writer.write(")");
  }

  @Override
  public void visit(LlvmSetccSD node) {
    writer.write("(");
    writer.write(node.lower() + " ");

    joinArgumentsWithComma(node.arguments());

    writer.write(")");
  }

  @Override
  public void visit(LlvmBasicBlockSD node) {
    writer.write(node.lower());
  }

  @Override
  public void visit(LlvmTargetCallSD node) {
    writer.write("(" + node.lower() + " ");

    joinArgumentsWithComma(node.arguments());

    writer.write(")");
  }

  @Override
  public void visit(LlvmBrindSD node) {
    writer.write("(" + node.lower() + " ");

    joinArgumentsWithComma(node.arguments());

    writer.write(")");
  }

  @Override
  public void visit(FieldAccessRefNode fieldAccessRefNode) {

  }

  @Override
  public void visit(AbstractBeginNode abstractBeginNode) {

  }

  @Override
  public void visit(InstrEndNode instrEndNode) {

  }

  @Override
  public void visit(ReturnNode returnNode) {

  }

  @Override
  public void visit(BranchEndNode branchEndNode) {

  }

  @Override
  public void visit(InstrCallNode instrCallNode) {

  }

  @Override
  public void visit(IfNode ifNode) {

  }

  @Override
  public void visit(ZeroExtendNode node) {

  }

  @Override
  public void visit(SignExtendNode node) {

  }

  @Override
  public void visit(TruncateNode node) {

  }

  @Override
  public void visit(LlvmBrSD node) {
    writer.write("(");
    writer.write(node.lower() + " ");
    visit(node.bb());
    writer.write(")");
  }

  @Override
  public void visit(LlvmBrCcSD node) {
    writer.write("(");
    writer.write(node.lower() + " ");
    writer.write(node.condition() + ", ");
    visit(node.first());
    writer.write(", ");
    visit(node.second());
    writer.write(", ");
    visit(node.immOffset());
    writer.write(")");
  }

  @Override
  public void visit(ExpressionNode expressionNode) {
    expressionNode.accept(this);
  }

  @Override
  public void visit(SideEffectNode sideEffectNode) {
    sideEffectNode.accept(this);
  }

  @Override
  public void visit(TupleGetFieldNode tupleGetFieldNode) {
    throw new RuntimeException("not implemented");
  }

  protected void joinArgumentsWithComma(NodeList<ExpressionNode> args) {
    for (int i = 0; i < args.size(); i++) {
      visit(args.get(i));

      if (i < args.size() - 1) {
        writer.write(", ");
      }
    }
  }
}
