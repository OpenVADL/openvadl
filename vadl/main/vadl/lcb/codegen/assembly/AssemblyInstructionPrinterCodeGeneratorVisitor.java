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

package vadl.lcb.codegen.assembly;

import static vadl.viam.ViamError.ensure;
import static vadl.viam.ViamError.ensurePresent;

import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.stream.Collectors;
import vadl.cppCodeGen.SymbolTable;
import vadl.error.Diagnostic;
import vadl.lcb.passes.llvmLowering.tablegen.model.ReferencesFormatField;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionBareSymbolOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionImmediateOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionLabelOperand;
import vadl.types.BuiltInTable;
import vadl.types.DataType;
import vadl.utils.SourceLocation;
import vadl.viam.Constant;
import vadl.viam.Format;
import vadl.viam.PrintableInstruction;
import vadl.viam.graph.Graph;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.HasRegisterFile;
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
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;
import vadl.viam.graph.dependency.ZeroExtendNode;

/**
 * Visitor for printing instructions for the assembler.
 */
public class AssemblyInstructionPrinterCodeGeneratorVisitor
    implements GraphNodeVisitor {
  private final PrintableInstruction instruction;
  private final SymbolTable symbolTable = new SymbolTable();
  private final StringWriter writer;
  private final Deque<String> operands = new ArrayDeque<>();
  private final TableGenInstruction tableGenInstruction;

  /**
   * Constructor.
   */
  public AssemblyInstructionPrinterCodeGeneratorVisitor(
      StringWriter writer,
      PrintableInstruction instruction,
      TableGenInstruction tableGenInstruction) {
    this.writer = writer;
    this.instruction = instruction;
    this.tableGenInstruction = tableGenInstruction;
  }

  @Override
  public void visit(ConstantNode node) {
    String symbol = symbolTable.getNextVariable();
    operands.add(symbol);

    if (node.constant() instanceof Constant.Str str) {
      writer.write("std::string " + symbol + " = std::string(\"" + str.value() + "\");\n");
    } else {
      throw Diagnostic.error("Not supported constant type", node.location()).build();
    }
  }

  @Override
  public void visit(BuiltInCall node) {
    if (node.builtIn() == BuiltInTable.MNEMONIC) {
      String symbol = symbolTable.getNextVariable();
      operands.add(symbol);

      writer.write(String.format("std::string %s = std::string(\"%s\");\n", symbol,
          instruction.identifier().simpleName()));
    } else if (node.builtIn() == BuiltInTable.CONCATENATE_STRINGS) {
      for (var arg : node.arguments()) {
        visit(arg);
      }

      String symbol = symbolTable.getNextVariable();
      writer.write("std::string " + symbol + " = ");
      for (int i = 0; i < node.arguments().size(); i++) {
        writer.write(operands.removeFirst());
        if (i < node.arguments().size() - 1) {
          writer.write(" + ");
        }
      }
      writer.write(";\n");
      operands.add(symbol);
    } else if (node.builtIn() == BuiltInTable.REGISTER) {
      ensure(node.arguments().size() == 1, "Expected only one argument");
      ensure(node.arguments().get(0) instanceof FieldRefNode,
          "Register argument is not a FieldRefNode");
      var cast = (FieldRefNode) node.arguments().get(0);
      var index = indexInInputs(cast.formatField())
          .or(() -> indexInOutputs(cast.formatField()))
          .orElseThrow(() -> Diagnostic.error(
              "Field is not part of an input or output operand in tablegen",
              cast.location()).build());
      var symbol = symbolTable.getNextVariable();

      // We need this helper function "getRegisterName..." because
      // we might need to support multiple register files.
      var registerFileName = getRegisterFile(instruction.behavior(), cast);
      writer.write(
          String.format(
              "std::string %s = "
                  + "getRegisterNameFrom%sByIndex("
                  + "MCOperandWrapper(MI->getOperand(%d)).unwrapToIntegral());" + " // "
                  + cast.formatField().identifier.simpleName() + "\n",
              symbol, registerFileName, index));
      operands.add(symbol);
    } else if (node.builtIn() == BuiltInTable.DECIMAL) {
      ensure(node.arguments().size() == 1, "Expected only one argument");
      writeImmediateWithRadix(node, 10);
    } else if (node.builtIn() == BuiltInTable.HEX) {
      ensure(node.arguments().size() == 1, "Expected only one argument");
      writeImmediateWithRadix(node, 16);
    } else {
      throw Diagnostic.error("Not supported builtin for assembly printing",
              node.location())
          .build();
    }
  }

  @Override
  public void visit(WriteRegTensorNode writeRegNode) {

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
    visit(returnNode.value());
    writer.write("return " + operands.pop() + ";\n");
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
  public void visit(ExpressionNode expressionNode) {
    expressionNode.accept(this);
  }

  @Override
  public void visit(SideEffectNode sideEffectNode) {

  }

  private void writeImmediateWithRadix(BuiltInCall node, int radix) {
    var type = node.arguments().getFirst().type().asDataType();

    if (node.arguments().get(0) instanceof FieldRefNode fieldRefNode) {
      writeImmediateWithRadix(fieldRefNode.formatField(), radix,
          type, fieldRefNode.location());
    } else if (node.arguments().get(0) instanceof FieldAccessRefNode fieldAccessRefNode) {
      writeImmediateWithRadix(fieldAccessRefNode.fieldAccess().fieldRef(), radix,
          type, fieldAccessRefNode.location());
    } else if (node.arguments().get(0) instanceof FuncParamNode funcParamNode) {
      // This case is for pseudo instructions because they arguments are not fields,
      // but function parameter nodes.
      writeImmediateWithRadix(funcParamNode, radix, funcParamNode.location());
    } else {
      throw Diagnostic.error("Not supported argument "
              + "in assembly printing", node.location())
          .build();
    }
  }

  private void writeImmediateWithRadix(FuncParamNode paramNode, int radix,
                                       SourceLocation sourceLocation) {
    var index = ensurePresent(indexInInputs(paramNode), () ->
        Diagnostic.error("Immediate must be part of an tablegen input.",
            sourceLocation)
    );

    var operandSymbol = symbolTable.getNextVariable();
    var valueSymbol = symbolTable.getNextVariable();

    writer.write(String.format(
        """
            MCOperand %s = MI->getOperand(%d);
            int64_t %s;
            if (AsmUtils::evaluateConstantImm(&%s, %s)) {
            """,
        operandSymbol,
        index,
        valueSymbol,
        operandSymbol,
        valueSymbol
    ));
    writer.write(String.format("\t%s =  MCOperand::createImm(%s);\n", operandSymbol, valueSymbol));
    writer.write("}\n");

    var symbol = symbolTable.getNextVariable();
    writer.write(String.format(
        """
            std::string %s = AsmUtils::formatImm(MCOperandWrapper(%s), %d, &MAI);
            """,
        symbol,
        operandSymbol,
        radix
    ));
    operands.add(symbol);
  }

  private void writeImmediateWithRadix(Format.Field field, int radix, DataType argumentType,
                                       SourceLocation sourceLocation) {
    var index = ensurePresent(indexInInputs(field), () ->
        Diagnostic.error("Immediate must be part of an tablegen input.",
            sourceLocation)
    );

    var operandSymbol = symbolTable.getNextVariable();
    var valueSymbol = symbolTable.getNextVariable();

    writer.write(String.format(
        """
            MCOperand %s = MI->getOperand(%d);
            int64_t %s;
            if (AsmUtils::evaluateConstantImm(&%s, %s)) {
            """,
        operandSymbol,
        index,
        valueSymbol,
        operandSymbol,
        valueSymbol
    ));


    var fieldAccesses = instruction.assembly().fieldAccesses().stream();
    if (fieldAccesses.noneMatch(fieldAccess -> fieldAccess.fieldRef().equals(field))) {
      var indexInInputs = index - tableGenInstruction.getOutOperands().size();
      var tableGenImmediate = tableGenInstruction.getInOperands().get(indexInInputs);
      var immediateRecord =
          ((TableGenInstructionImmediateOperand) tableGenImmediate).immediateOperand();
      var encodeMethod = immediateRecord.rawEncoderMethod();

      var encodedSymbol = symbolTable.getNextVariable();

      writer.write(String.format("\tauto %s = %s(%s);\n",
          encodedSymbol, encodeMethod, valueSymbol));

      if (argumentType.isSigned()) {
        var bitWith = argumentType.bitWidth();
        var bitsetSymbol = symbolTable.getNextVariable();

        writer.write(
            String.format("\tstd::bitset<%d> %s(%s);\n", bitWith, bitsetSymbol, encodedSymbol));

        writer.write(
            String.format("\tint64_t %s = signExtendBitset(%s);\n", valueSymbol, bitsetSymbol));

      } else {
        writer.write(String.format("\n%s = %s;\n", valueSymbol, encodedSymbol));
      }
    }


    writer.write(String.format("\t%s =  MCOperand::createImm(%s);\n", operandSymbol, valueSymbol));
    writer.write("}\n");

    var symbol = symbolTable.getNextVariable();
    writer.write(String.format(
        """
            std::string %s = AsmUtils::formatImm(MCOperandWrapper(%s), %d, &MAI);
            """,
        symbol,
        operandSymbol,
        radix
    ));
    operands.add(symbol);
  }

  private Optional<Integer> indexInInputs(FuncParamNode needle) {
    if (tableGenInstruction.getInOperands().stream()
        .filter(x -> x instanceof TableGenInstructionLabelOperand).count() > 1) {
      // When we see an immediate label operand, we do not know which operand it is.
      // Therefore, the support is limited at the moment.
      throw Diagnostic.error("Currently we cannot support multiple labels when printing",
          needle.location()).build();
    }

    if (tableGenInstruction.getInOperands().stream()
        .anyMatch(x -> x instanceof TableGenInstructionLabelOperand)
        && tableGenInstruction.getInOperands().size() > 1) {
      // When we see an immediate label operand, we do not know which operand it is.
      // Therefore, the support is limited at the moment.
      throw Diagnostic.error("Currently we cannot support mixed labels when printing",
          needle.location()).build();
    }

    int outputOffset = tableGenInstruction.getOutOperands().size();
    for (int i = 0; i < tableGenInstruction.getInOperands().size(); i++) {
      var operand = tableGenInstruction.getInOperands().get(i);
      if (operand instanceof TableGenInstructionBareSymbolOperand symbolOperand
          && symbolOperand.origin() instanceof FuncParamNode funcParamNodeOfOperand
          && needle.parameter().equals(funcParamNodeOfOperand.parameter())) {
        return Optional.of(outputOffset + i);
      } else if (operand instanceof TableGenInstructionLabelOperand) {
        return Optional.of(outputOffset + i);
      }
    }

    return Optional.empty();
  }

  private Optional<Integer> indexInInputs(Format.Field needle) {
    int outputOffset = tableGenInstruction.getOutOperands().size();
    for (int i = 0; i < tableGenInstruction.getInOperands().size(); i++) {
      var operand = tableGenInstruction.getInOperands().get(i);
      if (operand instanceof ReferencesFormatField x
          && x.formatField().equals(needle)) {
        return Optional.of(outputOffset + i);
      }
    }

    return Optional.empty();
  }

  private Optional<Integer> indexInOutputs(Format.Field needle) {
    for (int i = 0; i < tableGenInstruction.getOutOperands().size(); i++) {
      var operand = tableGenInstruction.getOutOperands().get(i);
      if (operand instanceof ReferencesFormatField x
          && x.formatField().equals(needle)) {
        return Optional.of(i);
      }
    }

    return Optional.empty();
  }

  private String getRegisterFile(Graph behavior, FieldRefNode fieldRefNode) {
    var candidates = behavior.getNodes(FieldRefNode.class)
        .filter(x -> x.formatField().equals(fieldRefNode.formatField()))
        .flatMap(Node::usages)
        .filter(x -> x instanceof HasRegisterFile y && y.hasRegisterFile())
        .map(x -> (HasRegisterFile) x)
        .collect(Collectors.toSet());

    if (candidates.isEmpty()) {
      throw Diagnostic.error(
          "Field is not used as register. Therefore the compiler generator cannot "
              +
              "detect the register file.",
          fieldRefNode.location()).build();
    } else if (candidates.size() > 1) {
      throw Diagnostic.error(
          "Field is by multiple register file. Therefore the compiler generator cannot "
              +
              "detect the register file for the assembly.",
          fieldRefNode.location()).build();
    } else {
      return candidates.iterator().next().registerFile().identifier.simpleName();
    }
  }
}