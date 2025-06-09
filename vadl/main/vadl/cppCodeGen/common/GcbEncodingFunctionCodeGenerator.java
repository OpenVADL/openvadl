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

package vadl.cppCodeGen.common;

import static vadl.error.DiagUtils.throwNotAllowed;

import vadl.cppCodeGen.CppTypeMap;
import vadl.cppCodeGen.FunctionCodeGenerator;
import vadl.cppCodeGen.context.CGenContext;
import vadl.types.BitsType;
import vadl.viam.Function;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.AsmBuiltInCall;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.ReadArtificialResNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.SliceNode;

/**
 * It generates code for encodings fields from instructions.
 */
public class GcbEncodingFunctionCodeGenerator extends FunctionCodeGenerator {

  private final String functionName;

  /**
   * Constructor.
   */
  public GcbEncodingFunctionCodeGenerator(
      Function function) {
    super(function);
    this.functionName = function.identifier.lower();
  }

  @Override
  protected void handle(CGenContext<Node> ctx, ReadRegTensorNode toHandle) {
    throwNotAllowed(toHandle, "Register reads");
  }

  @Override
  protected void handle(CGenContext<Node> ctx, ReadMemNode toHandle) {
    throwNotAllowed(toHandle, "Memory reads");
  }

  @Override
  protected void handle(CGenContext<Node> ctx, ReadArtificialResNode toHandle) {
    throwNotAllowed(toHandle, "Artificial resource reads");
  }

  @Override
  protected void handle(CGenContext<Node> ctx, AsmBuiltInCall toHandle) {
    throwNotAllowed(toHandle, "Asm builtin calls");
  }

  @Override
  public void handle(CGenContext<Node> ctx, SliceNode toHandle) {
    var parts = toHandle.bitSlice().parts().toList();
    ctx.wr("(");

    int acc = 0;
    for (int i = parts.size() - 1; i >= 0; i--) {
      if (i != parts.size() - 1) {
        ctx.wr(" | ");
      }

      var part = parts.get(i);
      var bitWidth = ((BitsType) toHandle.value().type()).bitWidth();
      if (part.isIndex()) {
        ctx.wr(
            String.format("project_range<%d, %d>(std::bitset<%d>(", part.lsb(), part.msb(),
                bitWidth));
        ctx.gen(toHandle.value()); // same expression
        ctx.wr(String.format(")) << %d", acc));
      } else {
        ctx.wr(
            String.format("project_range<%d, %d>(std::bitset<%d>(", part.lsb(), part.msb(),
                bitWidth));
        ctx.gen(toHandle.value()); // same expression
        ctx.wr(String.format(")) << %d", acc));
      }

      acc += part.msb() - part.lsb() + 1;
    }
    ctx.wr(").to_ulong()");
  }

  @Override
  public void handle(CGenContext<Node> ctx, FuncParamNode toHandle) {
    ctx.wr(toHandle.parameter().simpleName());
  }

  @Override
  protected void handle(CGenContext<Node> ctx, FieldAccessRefNode toHandle) {
    // We can simply use the accessFunction's name because
    // generated variables to reference between from the MCInst.
    ctx.wr(toHandle.fieldAccess().simpleName());
  }

  @Override
  protected void handle(CGenContext<Node> ctx, FieldRefNode toHandle) {
    throwNotAllowed(toHandle, "Asm builtin calls");
  }

  /*
  @Override
  public String genFunctionDefinition() {
    var returnNode = getSingleNode(function().behavior(), ReturnNode.class);

    context().wr(genFunctionSignature())
        .wr(" {\n");

    var merged =
        Stream.concat(tableGenInstruction.getOutOperands().stream(),
                tableGenInstruction.getInOperands().stream())
            .toList();

    var matched = false;
    for (var operand : tableGenInstruction.getInOperands()) {
      if (operand instanceof TableGenInstructionImmediateOperand immediateOperand) {
        writeImmediateOperand(operand, immediateOperand.immediateOperand(), merged);
        matched = true;
      } else if (operand instanceof TableGenInstructionLabelOperand labelOperand) {
        writeImmediateOperand(operand, labelOperand.immediateOperand(), merged);
        matched = true;
      }
    }

    if(!matched) {
      // It might happen that an instruction has field access functions but do not use them.
      // For example in RISCV MULH has the `shamt` field access function, but doesn't use it.

      context().wr("   llvm_unreachable(\"unusable encoding\");")
          .wr("\n}")
          .ln();

      return builder().toString();
    }

    context
        .wr("   return ")
        .gen(returnNode.value())
        .wr(";\n}");
    return builder().toString();
  }

  private void writeImmediateOperand(TableGenInstructionOperand operand,
                                     TableGenImmediateRecord immediateRecord,
                                     List<TableGenInstructionOperand> merged) {
    var index = merged.indexOf(operand);
    context.wr("   auto %s = MI.getOperand(%d);",
        immediateRecord.fieldAccessRef().simpleName(),
        index).ln();
  }

  private void writeImmediateOperand(TableGenInstructionBareSymbolOperand operand,
                                     List<TableGenInstructionOperand> merged) {
    var index = merged.indexOf(operand);
    context.wr("   auto %s = MI.getOperand(%d);",
        operand.name(),
        index).ln();
  }
   */

  @Override
  public String genFunctionName() {
    return functionName;
  }

  @Override
  public String genFunctionSignature() {
    var returnType = function.returnType().asDataType().fittingCppType();

    function.ensure(returnType != null, "No fitting Cpp type found for return type %s", returnType);
    function.ensure(function.behavior().isPureFunction(), "Function is not pure.");

    return CppTypeMap.getCppTypeNameByVadlType(returnType)
        + " %s(%s)".formatted(functionName, genFunctionParameters(function().parameters()));
  }
}
