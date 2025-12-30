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

package vadl.iss.passes.opDecomposition;

import static vadl.utils.StreamUtils.only;

import com.google.errorprone.annotations.concurrent.LazyInit;
import javax.annotation.Nullable;
import vadl.iss.passes.opDecomposition.decomposer.ShiftDecomposer;
import vadl.javaannotations.DispatchFor;
import vadl.javaannotations.Handler;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.utils.GraphUtils;
import vadl.utils.VadlBuiltInEmptyNoStatusDispatcher;
import vadl.viam.Constant;
import vadl.viam.graph.control.AbstractEndNode;
import vadl.viam.graph.dependency.AsmBuiltInCall;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.DynSliceNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FoldNode;
import vadl.viam.graph.dependency.ForIdxNode;
import vadl.viam.graph.dependency.FuncCallNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.LabelNode;
import vadl.viam.graph.dependency.LetNode;
import vadl.viam.graph.dependency.MiaBuiltInCall;
import vadl.viam.graph.dependency.ReadArtificialResNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.ReadSignalNode;
import vadl.viam.graph.dependency.ReadStageOutputNode;
import vadl.viam.graph.dependency.SelectNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.TensorNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.TupleGetFieldNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;
import vadl.viam.graph.dependency.ZeroExtendNode;

/**
 * The decomposer is designed as a request system, where one node requests
 * a slice from a dependency node.
 * The slice's width must be smaller/equal than the target size.
 * The request is dispatched to the correct implementation, which than takes care of
 * returning an expression that represents the result of the requested slice.
 * It must only use operations that are smaller equal the target size.
 */
@DispatchFor(
    value = ExpressionNode.class,
    include = "vadl.viam",
    context = Decomposer.Request.class
)
@SuppressWarnings("OverloadMethodsDeclarationOrder")
class Decomposer
    implements VadlBuiltInEmptyNoStatusDispatcher<Decomposer.Request>, ShiftDecomposer {

  record Slice(int hi, int lo) {
    int width() {
      return hi - lo + 1;
    }

    Slice shift(int n) {
      return new Slice(hi + n, lo + n);
    }
  }

  static class Request {
    Slice slice;
    /**
     * The result of the decomposition request.
     * The passed result will be used to replace the original expression.
     */
    @Nullable
    ExpressionNode result;

    public Request(Slice slice) {
      this.slice = slice;
    }
  }

  @LazyInit
  BuiltInCall currCall;
  int targetSize;

  public Decomposer(int targetSize) {
    this.targetSize = targetSize;
  }

  /**
   * Decompose the expression by requesting its substitute and replace it by the substitute.
   * The return type of the requested node must be <= the target size.
   *
   * @param expr the expression to decompose (will be replaced by the decomposed version)
   */
  void decompose(ExpressionNode expr) {
    var exprW = expr.type().asDataType().bitWidth();
    // we must start a decomposition with a node that has a valid return type size
    expr.ensure(exprW <= targetSize, "Can only decompose expr that fits in target size.");
    // start the request with the expressions' normal size
    var repl = internalRequest(expr, new Slice(exprW - 1, 0));
    if (repl == expr) {
      // if the replacement is the same as the original, we can skip the replace
      return;
    }
    expr.replaceAndDelete(repl);
  }

  void decompose(SideEffectNode sideEffect) {
    switch (sideEffect) {
      case WriteMemNode w -> handle(w);
      case WriteRegTensorNode w -> handle(w);
      default -> sideEffect.fail("Unexpected side effect to handle: %s", sideEffect.getClass());
    }
  }

  @Override
  public int targetSize() {
    return targetSize;
  }

  @Override
  public ExpressionNode request(ExpressionNode node, int msb, int lsb) {
    return request(node, new Slice(msb, lsb));
  }

  /**
   * Helper to extract a slice from a chunk and accumulate it into the result.
   * Handles source location copying and concatenation logic.
   */
  private ExpressionNode accumulateChunk(
      @Nullable
      ExpressionNode result,
      ExpressionNode chunkRead,
      Slice requestedSlice,
      int chunkLsb,
      int chunkWidth,
      ExpressionNode sourceNode
  ) {
    chunkRead.setSourceLocation(sourceNode.location());

    // Extract the requested slice from this chunk
    int sliceInChunk_lo = Math.max(0, requestedSlice.lo() - chunkLsb);
    int sliceInChunk_hi = Math.min(chunkWidth - 1, requestedSlice.hi() - chunkLsb);

    var chunkPart = new SliceNode(chunkRead,
        Constant.BitSlice.of(sliceInChunk_hi, sliceInChunk_lo),
        Type.bits(sliceInChunk_hi - sliceInChunk_lo + 1));

    // Concatenate chunks (or use single chunk if that's all we need)
    return result == null ? chunkPart : GraphUtils.concat(chunkPart, result);
  }

  private ExpressionNode request(ExpressionNode node, Slice slice) {
    if (slice.lo() == 0 && node.type().asDataType().bitWidth() <= targetSize) {
      // if the slice is just a truncate and the node does fit in the target size, we
      // can just truncate and return it.
      if (node.type().asDataType().bitWidth() - 1 == slice.hi()) {
        return node;
      } else {
        return GraphUtils.truncate(node, Type.bits(slice.width()));
      }
    }

    return internalRequest(node, slice);
  }

  private ExpressionNode internalRequest(ExpressionNode node, Slice slice) {
    var req = new Request(slice);
    DecomposerDispatcher.dispatch(this, req, node);
    if (req.result == null) {
      throw new IllegalStateException("Not yet implemented: " + node);
    }
    return req.result;
  }

  void handle(WriteMemNode write) {
    int writeWidth = write.writeBitWidth();
    if (writeWidth <= targetSize) {
      return; // fits in target size, no decomposition needed
    }

    var memory = write.memory();
    var wordSize = memory.wordSize();
    var address = write.address();
    var value = write.value();
    var condition = write.nullableCondition();
    var graph = write.ensureGraph();

    var ends = write.usages().gather(only(AbstractEndNode.class)).toList();

    // Calculate how many target-sized chunks we need
    int chunksNeeded = (writeWidth + targetSize - 1) / targetSize;

    // Split the write into multiple smaller writes
    for (int i = 0; i < chunksNeeded; i++) {
      int lsb = i * targetSize;
      int msb = Math.min(lsb + targetSize - 1, writeWidth - 1);
      int chunkWidth = msb - lsb + 1;

      // Extract slice of the value
      var chunkValue = request(value, msb, lsb);

      // Calculate address offset (in words)
      int byteOffset = lsb / 8;
      int wordOffset = byteOffset / (wordSize / 8);

      // Create address with offset
      var offsetConst = Constant.Value.of(
          wordOffset,
          address.type().asDataType()
      ).toNode();
      var adjustedAddress = wordOffset == 0 ? address :
          graph.addWithInputs(BuiltInTable.ADD.call(address, offsetConst));

      // Calculate words for this chunk
      int chunkWords = (chunkWidth + wordSize - 1) / wordSize;

      // Create new WriteMemNode for this chunk
      var chunkWrite = graph.addWithInputs(
          new WriteMemNode(memory, chunkWords, adjustedAddress, chunkValue, condition));

      // Preserve source location
      chunkWrite.setSourceLocation(write.location());

      for (var end : ends) {
        end.addSideEffect(chunkWrite);
      }
    }

    // Delete the original large write
    for (var end : ends) {
      end.removeSideEffect(write);
    }
    write.safeDelete();
  }

  void handle(WriteRegTensorNode write) {
    int writeWidth = write.writeBitWidth();
    if (writeWidth <= targetSize) {
      return; // fits in target size, no decomposition needed
    }

    var regTensor = write.regTensor();
    var indices = write.indices();
    var value = write.value();
    var condition = write.nullableCondition();
    var staticCounterAccess = write.staticCounterAccess();
    var graph = write.ensureGraph();

    var ends = write.usages().gather(only(AbstractEndNode.class)).toList();

    // Always decompose to innermost dimension (individual registers)
    int innermostSize = regTensor.innermostDim().size();
    int maxIndices = regTensor.maxNumberOfAccessIndices();

    // Calculate how many innermost registers we need to write
    int registersNeeded = writeWidth / innermostSize;
    write.ensure(writeWidth % innermostSize == 0,
        "Write width %d is not a multiple of innermost dimension size %d",
        writeWidth, innermostSize);

    // Calculate dimension sizes for multi-dimensional indexing
    var dims = regTensor.dimensions();
    int[] dimSizes = new int[maxIndices];
    for (int d = 0; d < maxIndices; d++) {
      dimSizes[d] = dims.get(d).size();
    }

    // Split the write into multiple writes to individual innermost registers
    for (int i = 0; i < registersNeeded; i++) {
      int lsb = i * innermostSize;
      int msb = lsb + innermostSize - 1;

      // Extract slice of the value for this innermost register
      var chunkValue = request(value, msb, lsb);

      // Calculate multi-dimensional index from linear index i
      var newIndices = indices.copy();
      int remaining = i;
      for (int d = indices.size(); d < maxIndices; d++) {
        int dimIndex = remaining % dimSizes[d];
        remaining /= dimSizes[d];
        var indexConst = Constant.Value.of(dimIndex, dims.get(d).indexType()).toNode();
        newIndices.add(indexConst);
      }

      // Create new WriteRegTensorNode for this innermost register
      var chunkWrite = graph.addWithInputs(
          new WriteRegTensorNode(regTensor, newIndices, chunkValue, staticCounterAccess,
              condition));

      // Preserve source location
      chunkWrite.setSourceLocation(write.location());

      for (var end : ends) {
        end.addSideEffect(chunkWrite);
      }
    }

    // Delete the original large write
    for (var end : ends) {
      end.removeSideEffect(write);
    }
    write.safeDelete();
  }

  @Override
  public void handleConcat(Request req) {
    var lsb = req.slice.lo();
    var msb = req.slice.hi();
    var loVal = currCall.arg(1);
    var hiVal = currCall.arg(0);
    var loValWidth = loVal.type().asDataType().bitWidth();
    if (lsb >= loValWidth) {               // slice lies completely in HI part
      req.result = request(hiVal, req.slice.shift(-loValWidth));
    } else if (msb < loValWidth) {         // slice lies completely in LO part
      req.result = request(loVal, req.slice);
    } else {                               // slice spans the boundary
      var loSlice = new Slice(loValWidth - 1, lsb);           // 63…lsb
      var hiSlice = new Slice(msb - loValWidth, 0);           // (msb-64)…0
      var newLo = request(loVal, loSlice);
      var newHi = request(hiVal, hiSlice);
      req.result = GraphUtils.concat(newHi, newLo);
    }
  }


  @Handler
  void handle(Request rq, TruncateNode t) {
    // width of the truncated value (= result width)
    int outW = t.type().asDataType().bitWidth();

    // slice must lie completely inside the truncated range
    if (rq.slice.hi() >= outW) {
      throw new IllegalArgumentException(
          "requested bits " + rq.slice + " exceed " + outW + "-bit truncate");
    }

    /* forward the same slice to the input of the truncate */
    rq.result = request(t.value(), rq.slice);
  }

  @Handler
  void handle(Request rq, SignExtendNode se) {
    int fromW = se.fromBitWidth();       // width before extension
    int lsb = rq.slice.lo();
    int msb = rq.slice.hi();
    var src = se.value();                // original value

    if (msb < fromW) {
      // slice lies completely inside the original value
      rq.result = request(src, rq.slice);

    } else if (lsb >= fromW) {
      // slice lies completely in the replicated sign-bit area
      var signBit = request(src, new Slice(fromW - 1, fromW - 1));
      rq.result = GraphUtils.signExtend(signBit, Type.bits(rq.slice.width()));
    } else {
      // slice crosses the boundary
      var lowSlice = new Slice(fromW - 1, lsb);                // inside src
      var highSize = msb - fromW + 1;                          // #sign bits
      var lowPart = request(src, lowSlice);
      var signBit = request(src, new Slice(fromW - 1, fromW - 1));
      var highPart = GraphUtils.signExtend(signBit, Type.bits(highSize));
      rq.result = GraphUtils.concat(highPart, lowPart);     // (hi, lo)
    }
  }

  @Override
  public void handleLSR(Request rq) {
    rq.result = lsrDecompose(currCall, rq.slice.hi(), rq.slice.lo());
  }


  @Handler
  void handle(Request rq, MiaBuiltInCall toHandle) {
    throw new UnsupportedOperationException("Type MiaBuiltInCall not yet implemented");
  }

  @Handler
  void handle(Request rq, ReadRegTensorNode toHandle) {
    var regTensor = toHandle.regTensor();
    var indices = toHandle.indices();
    var staticCounterAccess = toHandle.staticCounterAccess();
    var readWidth = regTensor.resultType(indices.size()).bitWidth();

    // Ensure the requested slice is within bounds
    toHandle.ensure(rq.slice.hi() < readWidth,
        "Requested slice [%d:%d] exceeds register width of %d bits",
        rq.slice.hi(), rq.slice.lo(), readWidth);

    // If read fits in target size, just slice it
    if (readWidth <= targetSize) {
      rq.result = new SliceNode(toHandle,
          Constant.BitSlice.of(rq.slice.hi(), rq.slice.lo()),
          Type.bits(rq.slice.width()));
      return;
    }

    // Need to decompose: read one or more innermost registers
    int innermostSize = regTensor.innermostDim().size();
    int maxIndices = regTensor.maxNumberOfAccessIndices();

    // Calculate which innermost registers contain our requested slice
    int firstRegIdx = rq.slice.lo() / innermostSize;
    int lastRegIdx = rq.slice.hi() / innermostSize;

    // Calculate dimension sizes for multi-dimensional indexing
    var dims = regTensor.dimensions();
    int[] dimSizes = new int[maxIndices];
    for (int d = 0; d < maxIndices; d++) {
      dimSizes[d] = dims.get(d).size();
    }

    ExpressionNode result = null;

    // Read each innermost register that contains part of the requested slice
    for (int regIdx = firstRegIdx; regIdx <= lastRegIdx; regIdx++) {
      int regLsb = regIdx * innermostSize;

      // Calculate multi-dimensional index from linear index
      var newIndices = indices.copy();
      int remaining = regIdx;
      for (int d = indices.size(); d < maxIndices; d++) {
        int dimIndex = remaining % dimSizes[d];
        remaining /= dimSizes[d];
        var indexConst = Constant.Value.of(dimIndex, dims.get(d).indexType()).toNode();
        newIndices.add(indexConst);
      }

      // Read this innermost register
      var regRead = new ReadRegTensorNode(
          regTensor,
          newIndices,
          Type.bits(innermostSize).asDataType(),
          staticCounterAccess
      );

      result = accumulateChunk(result, regRead, rq.slice, regLsb, innermostSize, toHandle);
    }

    rq.result = result;
  }

  @Handler
  void handle(Request rq, FieldRefNode toHandle) {
    throw new UnsupportedOperationException("Type FieldRefNode not yet implemented");
  }

  @Handler
  void handle(Request rq, FuncCallNode toHandle) {
    throw new UnsupportedOperationException("Type FuncCallNode not yet implemented");
  }

  @Handler
  void handle(Request rq, ReadStageOutputNode toHandle) {
    throw new UnsupportedOperationException("Type ReadStageOutputNode not yet implemented");
  }

  @Handler
  void handle(Request rq, LabelNode toHandle) {
    throw new UnsupportedOperationException("Type LabelNode not yet implemented");
  }

  @Handler
  void handle(Request rq, BuiltInCall toHandle) {
    var previousCall = currCall;
    currCall = toHandle;
    dispatch(rq, toHandle.builtIn());
    currCall = previousCall;
  }

  @Handler
  void handle(Request rq, ConstantNode toHandle) {
    var val = toHandle.constant().asVal();
    var sliced = val.slice(Constant.BitSlice.of(rq.slice.hi(), rq.slice.lo()));
    rq.result = sliced.toNode();
  }

  @Handler
  void handle(Request rq, FieldAccessRefNode toHandle) {
    throw new UnsupportedOperationException("Type FieldAccessRefNode not yet implemented");
  }

  @Handler
  void handle(Request rq, ReadMemNode toHandle) {
    int readWidth = toHandle.readBitWidth();
    var memory = toHandle.memory();
    var wordSize = memory.wordSize();
    var address = toHandle.address();

    // If the entire read fits in target size, just slice it
    if (readWidth <= targetSize) {
      rq.result = new SliceNode(toHandle,
          Constant.BitSlice.of(rq.slice.hi(), rq.slice.lo()),
          Type.bits(rq.slice.width()));
      return;
    }

    // Determine which chunks contain our requested slice
    int chunkSize = targetSize;
    int chunkStartIdx = rq.slice.lo() / chunkSize;
    int chunkEndIdx = rq.slice.hi() / chunkSize;

    var graph = toHandle.ensureGraph();
    ExpressionNode result = null;

    // Process all chunks that contain parts of the requested slice
    for (int chunkIdx = chunkStartIdx; chunkIdx <= chunkEndIdx; chunkIdx++) {
      int chunkLsb = chunkIdx * chunkSize;
      int chunkMsb = Math.min(chunkLsb + chunkSize - 1, readWidth - 1);
      int chunkWidth = chunkMsb - chunkLsb + 1;

      // Calculate address offset (in words)
      int byteOffset = chunkLsb / 8;
      int wordOffset = byteOffset / (wordSize / 8);
      int chunkWords = (chunkWidth + wordSize - 1) / wordSize;

      // Create address with offset
      var offsetConst = Constant.Value.of(wordOffset, address.type().asDataType()).toNode();
      var adjustedAddress = wordOffset == 0 ? address :
          graph.addWithInputs(BuiltInTable.ADD.call(address, offsetConst));

      // Create read for this chunk
      var chunkRead = new ReadMemNode(memory, chunkWords, adjustedAddress,
          Type.bits(chunkWidth).asDataType());

      result = accumulateChunk(result, chunkRead, rq.slice, chunkLsb, chunkWidth, toHandle);
    }

    rq.result = result;
  }

  @Handler
  void handle(Request rq, ReadArtificialResNode toHandle) {
    throw new UnsupportedOperationException("Type ReadArtificialResNode not yet implemented");
  }

  @Handler
  void handle(Request rq, TupleGetFieldNode toHandle) {
    throw new UnsupportedOperationException("Type TupleGetFieldNode not yet implemented");
  }

  @Handler
  void handle(Request rq, AsmBuiltInCall toHandle) {
    throw new UnsupportedOperationException("Type AsmBuiltInCall not yet implemented");
  }

  @Handler
  void handle(Request rq, FuncParamNode toHandle) {
    throw new UnsupportedOperationException("Type FuncParamNode not yet implemented");
  }

  @Handler
  void handle(Request rq, ZeroExtendNode toHandle) {
    throw new UnsupportedOperationException("Type ZeroExtendNode not yet implemented");
  }

  @Handler
  void handle(Request rq, LetNode toHandle) {
    throw new UnsupportedOperationException("Type LetNode not yet implemented");
  }

  @Handler
  void handle(Request rq, SliceNode toHandle) {
    var bitSlice = toHandle.bitSlice();
    toHandle.ensure(bitSlice.isContinuous(),
        "Decomposing slices with multiple parts is not yet implemented");

    var slice = bitSlice.parts().findFirst().get();
    // the request slice defines indices on the slice node's result value type.
    // we can normalize this to the slice node's value type by shifting the indices
    // by the slice node's lsb value, such that the requested window stays the same
    // while removing the intermediate slice node.
    var newReqSlice = rq.slice.shift(slice.lsb());

    // we request the adjusted request slice from the slice node's value.
    rq.result = request(toHandle.value(), newReqSlice);
  }

  @Handler
  void handle(Request rq, DynSliceNode toHandle) {
    throw new UnsupportedOperationException("Type DynSliceNode not yet implemented");
  }

  @Handler
  void handle(Request rq, SelectNode toHandle) {
    throw new UnsupportedOperationException("Type SelectNode not yet implemented");
  }

  @Handler
  void handle(Request rq, FoldNode toHandle) {
    throw new UnsupportedOperationException("Type FoldNode not yet implemented");
  }

  @Handler
  void handle(Request rq, ForIdxNode toHandle) {
    throw new UnsupportedOperationException("Type ForIdxNode not yet implemented");
  }

  @Handler
  void handle(Request rq, TensorNode toHandle) {
    throw new UnsupportedOperationException("Type TensorNode not yet implemented");
  }

  @Handler
  void handle(Request rq, ReadSignalNode toHandle) {
    throw new UnsupportedOperationException("Type ReadSignalNode not yet implemented");
  }

}