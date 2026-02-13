// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

import static vadl.cppCodeGen.CppTypeMap.getCppTypeNameByVadlType;
import static vadl.utils.GraphUtils.getSingleNode;

import vadl.cppCodeGen.common.PureFunctionCodeGenerator;
import vadl.cppCodeGen.context.CGenContext;
import vadl.types.BuiltInTable;
import vadl.viam.Function;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.FoldNode;
import vadl.viam.graph.dependency.ForIdxNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.TensorNode;

/**
 * A code generator class responsible for producing C function definitions that interact
 * with memory and register tensors for CPU-specific behavior. This class extends the
 * `PureFunctionCodeGenerator` and implements interfaces for reading/writing memory and
 * writing registers.
 */
public class IssCpuFunctionGenerator extends PureFunctionCodeGenerator
    implements IssCMixins.CpuSourceReadWriteMemory, IssCMixins.CpuSourceWriteRegTensor {
  /**
   * Creates a new pure function code generator for the specified function.
   *
   * @param function the function for which code should be generated
   */
  public IssCpuFunctionGenerator(Function function) {
    super(function);
  }

  /**
   * Generates a C function definition for the given {@link Function} in the context
   * of a helper call. So the CPU state is passed as first argument.
   */
  public String fetch(String cpuStateName) {
    var ctx = context();
    var returnNode = getSingleNode(function.behavior(), ReturnNode.class);
    var resultType = getCppTypeNameByVadlType(function.returnType());
    ctx.wr("static " + resultType + " " + genFunctionName() + "(" + cpuStateName
            + "* env, " + genFunctionParameters(function.parameters()) + ")"
        ).ln("   {")
        .spacedIn();
    if (returnNode.value() instanceof TensorNode tensorNode) {
      ctx.gen(tensorNode);
      ctx.ln().ln("return result;");
    } else if (returnNode.value() instanceof FoldNode foldNode) {
      ctx.gen(foldNode);
      ctx.ln().ln("return result;");
    } else {
      ctx.wr("return ").gen(returnNode.value()).ln(";");
    }
    ctx.spaceOut()
        .ln("}");
    return builder().toString();
  }

  @Override
  public void handle(CGenContext<Node> ctx, ReadMemNode node) {
    IssCMixins.CpuSourceReadWriteMemory.super.handle(ctx, node);
  }

  @Override
  public void handle(CGenContext<Node> ctx, ReadRegTensorNode node) {
    IssCMixins.CpuSourceWriteRegTensor.super.handle(ctx, node);
  }

  @Override
  public void handle(CGenContext<Node> ctx, ForIdxNode toHandle) {
    ctx.wr("i");
  }

  @Override
  protected void handle(CGenContext<Node> ctx, TensorNode toHandle) {
    toHandle.ensure(toHandle.usageCount() == 1
            && toHandle.usages().findFirst().get() instanceof ReturnNode,
        "The tensor is not a direct dependent of the return node. "
            + "So it is not part of a extracted C function as it should be "
            + "(IssCFunctionExtractorPass)."
    );

    var from = toHandle.idx().fromIdx();
    var to = toHandle.idx().toIdx();
    toHandle.ensure(from >= 0 && to >= 0,
        "Tensor extraction requires non-negative loop bounds. Got from=%s, to=%s.",
        from, to);
    var cmp = from <= to ? "<=" : ">=";
    var cnt = from <= to ? "++" : "--";
    var resultType = getCppTypeNameByVadlType(function.returnType());
    var elemWidth = toHandle.body().type().asDataType().bitWidth();
    ctx.ln(resultType + " result = 0;");
    ctx.wr("for (int64_t i = %s; i %s %s; i%s)", from, cmp, to, cnt)
        .ln("{")
        .spacedIn()
        .wr(resultType + " v = ")
        .gen(toHandle.body())
        .ln(";")
        .ln("result |= v << (i * %s);", elemWidth)
        .spaceOut()
        .ln("}");
  }

  @Override
  protected void handle(CGenContext<Node> ctx, FoldNode toHandle) {
    toHandle.ensure(toHandle.usageCount() == 1
            && toHandle.usages().findFirst().get() instanceof ReturnNode,
        "The fold is not a direct dependent of the return node. "
            + "So it is not part of an extracted C function as it should be "
            + "(IssCFunctionExtractorPass)."
    );

    var from = toHandle.idx().fromIdx();
    var to = toHandle.idx().toIdx();
    var cmp = from <= to ? "<=" : ">=";
    var cnt = from <= to ? "++" : "--";
    var resultType = getCppTypeNameByVadlType(function.returnType());
    var combiner = foldCombiner(toHandle);
    var neutral = foldNeutral(combiner, resultType);
    var op = foldOperator(combiner);

    ctx.ln(resultType + " result = " + neutral + ";");
    ctx.wr("for (int64_t i = %s; i %s %s; i%s)", from, cmp, to, cnt)
        .ln("{")
        .spacedIn()
        .wr(resultType + " v = ")
        .gen(toHandle.body())
        .ln(";")
        .ln("result = (" + resultType + ") (result " + op + " v);")
        .spaceOut()
        .ln("}");
  }

  private BuiltInTable.BuiltIn foldCombiner(FoldNode fold) {
    var combinerReturn = getSingleNode(fold.combiner().behavior(), ReturnNode.class);
    fold.ensure(combinerReturn.value() instanceof BuiltInCall,
        "Expected fold combiner to return a BuiltInCall.");
    return ((BuiltInCall) combinerReturn.value()).builtIn();
  }

  private String foldOperator(BuiltInTable.BuiltIn combiner) {
    if (combiner == BuiltInTable.ADD) {
      return "+";
    }
    if (combiner == BuiltInTable.MUL) {
      return "*";
    }
    if (combiner == BuiltInTable.AND) {
      return "&";
    }
    if (combiner == BuiltInTable.OR) {
      return "|";
    }
    if (combiner == BuiltInTable.XOR) {
      return "^";
    }
    throw new IllegalStateException("Unsupported fold combiner: " + combiner.name());
  }

  private String foldNeutral(BuiltInTable.BuiltIn combiner, String resultType) {
    if (combiner == BuiltInTable.MUL) {
      return "((" + resultType + ")1)";
    }
    if (combiner == BuiltInTable.AND) {
      return "((" + resultType + ")~((" + resultType + ")0))";
    }
    if (combiner == BuiltInTable.ADD
        || combiner == BuiltInTable.OR
        || combiner == BuiltInTable.XOR) {
      return "((" + resultType + ")0)";
    }
    throw new IllegalStateException("Unsupported fold combiner: " + combiner.name());
  }
}
