package vadl.gcb.passes.encoding.strategies.impl;

import vadl.gcb.passes.encoding.strategies.EncodingGenerationStrategy;
import vadl.viam.Constant;
import vadl.viam.Format;
import vadl.viam.Parameter;
import vadl.viam.ViamError;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.SliceNode;

/**
 * A trivial immediate is where the field access function is simply an immediate.
 * format Utype : Inst =
 * {     imm    : Bits<20>
 * , rd     : Index
 * , opcode : Bits7
 * , immU = imm as UInt<32>
 * }
 * This class should compute the following encoding function automatically:
 * encode {
 * imm => immU(19..0)
 * }
 */
public class TrivialImmediateStrategy implements EncodingGenerationStrategy {
  @Override
  public boolean checkIfApplicable(Format.FieldAccess fieldAccess) {
    // Checks whether the behavior does not contain any {@link BuiltIn} or {@link SliceNode}.
    var behavior = fieldAccess.accessFunction().behavior();
    return behavior.getNodes()
        .filter(x -> x instanceof BuiltInCall || x instanceof SliceNode)
        .findAny().isEmpty();
  }

  @Override
  public void generateEncoding(Format.FieldAccess fieldAccess) {
    var parameter = setupEncodingForFieldAccess(fieldAccess);

    var fieldRef = fieldAccess.fieldRef();
    // The field takes up a certain slice.
    // But we need to take a slice of the immediate of the same size.
    var fieldAccessBitSlice = fieldRef.bitSlice();
    var invertedSlice = new Constant.BitSlice(new Constant.BitSlice.Part[] {
        Constant.BitSlice.Part.of(fieldAccessBitSlice.bitSize() - 1, 0)});
    var invertedSliceNode = new SliceNode(new FuncParamNode(
        parameter),
        invertedSlice,
        fieldRef.type());
    var returnNode = new ReturnNode(invertedSliceNode);
    var startNode = new StartNode(returnNode);

    var encoding = fieldAccess.encoding();
    if (encoding != null && encoding.behavior() != null) {
      encoding.behavior().addWithInputs(returnNode);
      encoding.behavior().add(startNode);
    } else {
      throw new ViamError("An encoding must already exist");
    }
  }
}
