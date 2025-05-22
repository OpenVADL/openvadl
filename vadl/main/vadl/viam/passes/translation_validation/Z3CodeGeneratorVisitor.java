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

package vadl.viam.passes.translation_validation;

import java.io.StringWriter;
import java.util.Objects;
import vadl.types.BitsType;
import vadl.types.BoolType;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.Memory;
import vadl.viam.ViamError;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
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
import vadl.viam.graph.dependency.UnaryNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;
import vadl.viam.graph.dependency.ZeroExtendNode;

/**
 * Visitor which lowers {@link Node} into z3 predicates.
 */
public class Z3CodeGeneratorVisitor implements GraphNodeVisitor {
  protected final StringWriter writer = new StringWriter();

  public String getResult() {
    return writer.toString();
  }

  @Override
  public void visit(ConstantNode node) {
    if (node.constant() instanceof Constant.Value value
        && value.type() instanceof BitsType bitsType) {
      writer.write(String.format("BitVecVal(%d, %d)",
          value.intValue(),
          bitsType.bitWidth()));
    } else if (node.constant() instanceof Constant.Value value
        && value.type() instanceof BoolType) {
      if (value.bool()) {
        writer.write("True");
      } else {
        writer.write("False");
      }
    } else {
      throw new ViamError("not implemented");
    }
  }


  @Override
  public void visit(BuiltInCall node) {
    var operator = Z3BuiltinTranslationMap.lower(node.builtIn());

    switch (operator.right()) {
      case INFIX -> {
        node.ensure(node.arguments().size() > 1,
            "This method only works for more than 1 arguments");
        for (int i = 0; i < node.arguments().size(); i++) {
          visit(node.arguments().get(i));

          // The last argument should not emit an operand.
          if (i < node.arguments().size() - 1) {
            writer.write(" " + operator.left().value() + " ");
          }
        }
      }
      case PREFIX -> {
        if (node.arguments().size() == 1) {
          writer.write(operator.left().value() + "(");
          visit(node.arguments().get(0));
          writer.write(operator.left().value() + ")");
        } else {
          writer.write(operator.left().value() + "(");
          for (int i = 0; i < node.arguments().size(); i++) {
            visit(node.arguments().get(i));

            // The last argument should not emit a comma
            if (i < node.arguments().size() - 1) {
              writer.write(", ");
            }
          }
          writer.write(")");
        }
      }
      default -> throw new ViamError("Operator not covered");
    }
  }

  @Override
  public void visit(WriteRegTensorNode writeRegNode) {
    visit(writeRegNode.value());
  }

  @Override
  public void visit(WriteMemNode writeMemNode) {
    // Z3's theory of array defines only `Store` for one position.
    // Usually, the memory will be defined for a byte. So writing
    // multiple bytes requires recursion.
    writeMemNode.ensureNonNull(writeMemNode.address(), "Address must not be null");
    writeMultipleBytes(writeMemNode.memory(), writeMemNode.address(), writeMemNode.value(),
        writeMemNode.words(), 0, 0);
  }

  @Override
  public void visit(SliceNode sliceNode) {
    writer.write("Extract("
        + sliceNode.bitSlice().msb() + ", "
        + sliceNode.bitSlice().lsb() + ", ");
    visit(sliceNode.value());
    writer.write(")");
  }

  @Override
  public void visit(SelectNode selectNode) {
    writer.write("If(");
    visit(selectNode.condition());
    writer.write(", ");
    visit(selectNode.trueCase());
    writer.write(", ");
    visit(selectNode.falseCase());
    writer.write(")");
  }

  @Override
  public void visit(ReadRegTensorNode readRegNode) {
    if (readRegNode.regTensor().isSingleRegister()) {
      writer.write(readRegNode.regTensor().identifier.simpleName());
    } else if (readRegNode.regTensor().isRegisterFile()) {
      // Do not write the register file because we actually care about the address.
      // a = X << X (wrong)
      // a = rs1 << rs2 (correct)
      writer.write("Select(" + readRegNode.regTensor().identifier.simpleName() + ", ");
      visit(readRegNode.indices().getFirst());
      writer.write(")");
    } else {
      throw new RuntimeException("not implemented");
    }
  }

  @Override
  public void visit(ReadMemNode readMemNode) {
    var mem = readMemNode.memory().identifier.simpleName();
    writer.write("Select(" + mem + ", ");
    visit(readMemNode.address());
    writer.write(")");
  }

  @Override
  public void visit(LetNode letNode) {
    visit(letNode.expression());
  }

  @Override
  public void visit(FuncParamNode funcParamNode) {
    writer.write(funcParamNode.parameter().identifier.simpleName());
  }

  @Override
  public void visit(FuncCallNode funcCallNode) {
    writer.write(funcCallNode.function().identifier.simpleName());
  }

  @Override
  public void visit(FieldRefNode fieldRefNode) {
    writer.write(fieldRefNode.formatField().identifier.simpleName());
  }

  @Override
  public void visit(FieldAccessRefNode fieldAccessRefNode) {
    writer.write(fieldAccessRefNode.fieldAccess().identifier.simpleName());
  }

  @Override
  public void visit(AbstractBeginNode abstractBeginNode) {
  }

  @Override
  public void visit(InstrEndNode instrEndNode) {

  }

  @Override
  public void visit(ReturnNode returnNode) {
    visit(returnNode.value());
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
  public void visit(SideEffectNode sideEffectNode) {
    sideEffectNode.accept(this);
  }

  @Override
  public void visit(TupleGetFieldNode tupleGetFieldNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(ZeroExtendNode node) {
    var diff = node.type().bitWidth() - getBitWidth(node.value().type());
    writer.write("ZeroExt(" + diff + ", ");
    wrapImplicitBooleanWhenTypeBoolean(node);
    writer.write(")");
  }


  @Override
  public void visit(SignExtendNode node) {
    var diff = node.type().bitWidth() - getBitWidth(node.value().type());
    writer.write("SignExt(" + diff + ", ");
    wrapImplicitBooleanWhenTypeBoolean(node);
    writer.write(")");
  }

  @Override
  public void visit(TruncateNode node) {
    var diff = getBitWidth(node.value().type()) - node.type().bitWidth() - 1;
    if (diff > 0) {
      writer.write("Extract(" + diff);
      writer.write(", 0, ");
      wrapImplicitBooleanWhenTypeBoolean(node);
      writer.write(")");
    } else {
      visit(node.value());
    }
  }

  @Override
  public void visit(ExpressionNode expressionNode) {
    expressionNode.accept(this);
  }

  private void writeMultipleBytes(Memory memory, ExpressionNode address, ExpressionNode value,
                                  int words, int wordsWritten, int low) {
    var mem = memory.identifier.simpleName();
    var hi = low + memory.wordSize() - 1;
    if (words == 1) {
      writer.write("Store(" + mem + ", ");
      visit(Objects.requireNonNull(address));
      writer.write(" + " + wordsWritten + ", ");
      writer.write("Extract(" + hi + ", " + low + ", ");
      visit(value);
      writer.write("))");
    } else if (words > 1) {
      writer.write("Store(");
      writeMultipleBytes(memory, address, value, words - 1, wordsWritten + 1,
          low + memory.wordSize());
      writer.write(", ");
      visit(Objects.requireNonNull(address));
      writer.write(" + " + wordsWritten + ", ");
      writer.write("Extract(" + hi + ", " + low + ", ");
      visit(value);
      writer.write("))");
    }
  }

  private int getBitWidth(Type type) {
    if (type instanceof BitsType b) {
      return b.bitWidth();
    } else if (type instanceof BoolType b) {
      return b.bitWidth();
    }

    throw new RuntimeException("not supported");
  }

  private void wrapImplicitBooleanWhenTypeBoolean(UnaryNode node) {
    if (node.value().type() instanceof BoolType) {
      writer.write("If((");
      visit(node.value());
      writer.write(String.format(") == True, BitVecVal(1, %s), BitVecVal(0, %s))",
          getBitWidth(node.type()), getBitWidth(node.type())));
    } else {
      visit(node.value());
    }
  }
}
