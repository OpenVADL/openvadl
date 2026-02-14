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
import vadl.iss.passes.extensions.RegInfo;
import vadl.types.BuiltInTable;
import vadl.viam.Function;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.DynSliceNode;
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
  public void handle(CGenContext<Node> ctx, DynSliceNode node) {
    if (!(node.value() instanceof ReadRegTensorNode read)) {
      emitDefaultDynSlice(ctx, node);
      return;
    }

    var readWidth = read.type().asDataType().bitWidth();
    if (readWidth <= 64) {
      emitDefaultDynSlice(ctx, node);
      return;
    }

    var resultWidth = node.type().asDataType().bitWidth();
    node.ensure(resultWidth <= 64,
        "Extracted dyn-slice result width must be <= 64 bits.");

    var chunkCount = (readWidth + 63) / 64;
    ctx.wr("(");

    var first = true;
    // Case 1: both bounds are in the same chunk.
    for (int chunk = 0; chunk < chunkCount; chunk++) {
      if (!first) {
        ctx.wr(" : ");
      }
      emitDynSliceSameChunkCondition(ctx, node, readWidth, chunk);
      ctx.wr(" ? ");
      emitDynSliceFromChunk(ctx, read, readWidth, chunk, node);
      first = false;
    }

    // Case 2: bounds cross exactly one 64-bit chunk boundary.
    // Since result width is <=64, at most one boundary can be crossed.
    for (int boundaryChunk = 0; boundaryChunk < chunkCount - 1; boundaryChunk++) {
      ctx.wr(" : ");
      emitDynSliceCrossBoundaryCondition(ctx, node, boundaryChunk);
      ctx.wr(" ? ");
      emitDynSliceCrossChunk(ctx, read, readWidth, boundaryChunk, node, resultWidth);
    }

    ctx.wr(" : ").wr("((")
        .wr(getCppTypeNameByVadlType(node.type()))
        .wr(") 0))");
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

  private void emitDefaultDynSlice(CGenContext<Node> ctx, DynSliceNode node) {
    ctx.wr("VADL_slice(")
        .gen(node.value())
        .wr(", %s, ", 1)
        .gen(node.msb())
        .wr(", ")
        .gen(node.lsb())
        .wr(")");
  }

  private void emitDynSliceSameChunkCondition(
      CGenContext<Node> ctx, DynSliceNode node, int readWidth, int chunkIndex) {
    int chunkLo = chunkIndex * 64;
    int chunkHi = Math.min(readWidth - 1, chunkLo + 63);
    ctx.wr("(");
    ctx.gen(node.lsb()).wr(" >= ").wr(Integer.toString(chunkLo));
    ctx.wr(" && ");
    ctx.gen(node.msb()).wr(" <= ").wr(Integer.toString(chunkHi));
    ctx.wr(")");
  }

  private void emitDynSliceCrossBoundaryCondition(
      CGenContext<Node> ctx, DynSliceNode node, int lowChunkIndex) {
    int boundary = (lowChunkIndex + 1) * 64;
    ctx.wr("(");
    ctx.gen(node.lsb()).wr(" < ").wr(Integer.toString(boundary));
    ctx.wr(" && ");
    ctx.gen(node.msb()).wr(" >= ").wr(Integer.toString(boundary));
    ctx.wr(")");
  }

  private void emitDynSliceFromChunk(
      CGenContext<Node> ctx,
      ReadRegTensorNode read,
      int readWidth,
      int chunkIndex,
      DynSliceNode node
  ) {
    int chunkOffset = chunkIndex * 64;
    int chunkWidth = Math.min(64, readWidth - chunkOffset);

    ctx.wr("VADL_slice(");
    emitRegChunkRead(ctx, read, chunkOffset, chunkWidth);
    ctx.wr(", 1, ");
    if (chunkOffset > 0) {
      ctx.wr("(").gen(node.msb()).wr(" - ").wr(Integer.toString(chunkOffset)).wr(")");
    } else {
      ctx.gen(node.msb());
    }
    ctx.wr(", ");
    if (chunkOffset > 0) {
      ctx.wr("(").gen(node.lsb()).wr(" - ").wr(Integer.toString(chunkOffset)).wr(")");
    } else {
      ctx.gen(node.lsb());
    }
    ctx.wr(")");
  }

  private void emitDynSliceCrossChunk(
      CGenContext<Node> ctx,
      ReadRegTensorNode read,
      int readWidth,
      int lowChunkIndex,
      DynSliceNode node,
      int resultWidth
  ) {
    int lowChunkOffset = lowChunkIndex * 64;
    int highChunkOffset = lowChunkOffset + 64;
    int lowChunkWidth = Math.min(64, readWidth - lowChunkOffset);
    int highChunkWidth = Math.min(64, readWidth - highChunkOffset);
    int lowChunkTop = lowChunkWidth - 1;
    int boundary = highChunkOffset;

    // crossing case: [msb:lsb] spans a chunk boundary.
    // compose from high and low chunk pieces using only <=64-bit operations.
    ctx.wr("VADL_slice((");
    ctx.wr("(VADL_slice(");
    emitRegChunkRead(ctx, read, highChunkOffset, highChunkWidth);
    ctx.wr(", 1, (").gen(node.msb()).wr(" - ").wr(Integer.toString(highChunkOffset))
        .wr("), 0) << (")
        .wr(Integer.toString(boundary))
        .wr(" - ")
        .gen(node.lsb())
        .wr("))");
    ctx.wr(" | ");
    ctx.wr("VADL_slice(");
    emitRegChunkRead(ctx, read, lowChunkOffset, lowChunkWidth);
    ctx.wr(", 1, ").wr(Integer.toString(lowChunkTop)).wr(", ");
    if (lowChunkOffset > 0) {
      ctx.wr("(").gen(node.lsb()).wr(" - ").wr(Integer.toString(lowChunkOffset)).wr(")");
    } else {
      ctx.gen(node.lsb());
    }
    ctx.wr(")");
    ctx.wr("), 1, ").wr(Integer.toString(resultWidth - 1)).wr(", 0)");
  }

  private void emitRegChunkRead(
      CGenContext<Node> ctx,
      ReadRegTensorNode read,
      int chunkOffsetBits,
      int chunkWidthBits
  ) {
    var containerWidth = read.regTensor().resultType(read.indices().size()).bitWidth();
    var pattern = RegInfo.AccessPattern.of(
        read,
        RegInfo.AccessType.READ,
        read.regTensor(),
        read.indices(),
        chunkWidthBits,
        containerWidth,
        chunkOffsetBits
    );

    ctx.wr(pattern.name()).wr("(env");
    for (var index : read.indices()) {
      ctx.wr(", ");
      ctx.gen(index);
    }
    ctx.wr(")");
  }
}
