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
    // ReadRegTensorNode reads from a register tensor.
    // The requested slice must lie within the register's bit width.
    // We create a new ReadRegTensorNode with the same indices and let the slice
    // extract the requested bits from the full register read.

    var regTensor = toHandle.regTensor();
    var fullWidth = regTensor.resultType(toHandle.indices().size()).bitWidth();

    // Ensure the requested slice is within bounds
    toHandle.ensure(rq.slice.hi() < fullWidth,
        "Requested slice [%d:%d] exceeds register width of %d bits",
        rq.slice.hi(), rq.slice.lo(), fullWidth);

    // If the slice is the full register and fits in target size, return as-is
    if (rq.slice.lo() == 0 && rq.slice.hi() == fullWidth - 1 && fullWidth <= targetSize) {
      rq.result = toHandle;
      return;
    }

    // Create a read of the full register
    var fullRead = new ReadRegTensorNode(
        regTensor,
        toHandle.indices(),
        Type.bits(fullWidth).asDataType(),
        toHandle.staticCounterAccess()
    );
    fullRead.setSourceLocation(toHandle.location());

    // If the full width fits in target size, just slice it
    if (fullWidth <= targetSize) {
      rq.result = new SliceNode(fullRead,
          Constant.BitSlice.of(rq.slice.hi(), rq.slice.lo()),
          Type.bits(rq.slice.width()));
    } else {
      // Register is too wide - we need to decompose it
      // For now, we still need to slice from the original read,
      // but we request the decomposition of that slice
      var sliced = new SliceNode(fullRead,
          Constant.BitSlice.of(rq.slice.hi(), rq.slice.lo()),
          Type.bits(rq.slice.width()));

      // The sliced result should fit in target size now
      sliced.ensure(sliced.type().asDataType().bitWidth() <= targetSize,
          "Sliced register read still exceeds target size");

      rq.result = sliced;
    }
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
    throw new UnsupportedOperationException("Type ReadMemNode not yet implemented");
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
    if (toHandle.value() instanceof ReadRegTensorNode) {
      rq.result = toHandle;
      return;
    }
    throw new UnsupportedOperationException("Type SliceNode not yet implemented");
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