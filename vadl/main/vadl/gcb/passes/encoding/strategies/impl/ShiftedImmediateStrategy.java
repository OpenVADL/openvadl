package vadl.gcb.passes.encoding.strategies.impl;

import vadl.gcb.passes.encoding.strategies.EncodingGenerationStrategy;
import vadl.types.BuiltInTable;
import vadl.viam.Constant;
import vadl.viam.Format;
import vadl.viam.Parameter;
import vadl.viam.ViamError;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.SliceNode;

/**
 * This strategy will create an encoding when the immediate is shifted.
 * format Utype : Inst =
 * {     imm    : Bits<20>
 * , rd     : Index
 * , opcode : Bits7
 * , ImmediateU = ( imm, 0 as Bits<12> ) as UInt
 * }
 * This class should compute the following encoding function automatically:
 * encode {
 * imm => ImmediateU(31..12)
 * }
 */
public class ShiftedImmediateStrategy implements EncodingGenerationStrategy {
  @Override
  public boolean checkIfApplicable(Format.FieldAccess fieldAccess) {
    // Checks whether the behavior only contains (logical or arithmetic) left or right shift.
    // But only one logical operation is allowed.
    var behavior = fieldAccess.accessFunction().behavior();
    return behavior.getNodes(BuiltInCall.class)
        .allMatch(x -> {
          var cast = (BuiltInCall) x;

          if (cast.builtIn() == BuiltInTable.LSL
              || cast.builtIn() == BuiltInTable.LSR
              || cast.builtIn() == BuiltInTable.ASR) {
            return true;
          }

          return false;
        }) && behavior.getNodes(BuiltInCall.class).count() == 1;
  }

  @Override
  public void generateEncoding(Format.FieldAccess fieldAccess) {
    var parameter = setupEncodingForFieldAccess(fieldAccess);
    var accessFunction = fieldAccess.accessFunction();
    var fieldRef = fieldAccess.fieldRef();

    var originalShift =
        (BuiltInCall) accessFunction.behavior().getNodes(BuiltInCall.class).findFirst().get();
    var shiftValue =
        ((Constant.Value) ((ConstantNode) originalShift.arguments()
            .get(1)).constant()).integer();

    ExpressionNode invertedSliceNode;
    if (originalShift.builtIn() == BuiltInTable.LSL) {
      // If the decode function has a left shift,
      // then we need to extract the original shifted value.
      // We compute an upper bound which is the shift value plus the size of the field
      // and a lower bound which is the shifted value.
      var upperBound = shiftValue.intValue() + fieldRef.size() - 1;
      var lowerBound = shiftValue.intValue();
      var slice = new Constant.BitSlice(
          new Constant.BitSlice.Part[] {
              Constant.BitSlice.Part.of(upperBound, lowerBound)});
      invertedSliceNode = new SliceNode(new FuncParamNode(parameter), slice, fieldRef.type());
    } else if (originalShift.builtIn() == BuiltInTable.LSR
        || originalShift.builtIn() == BuiltInTable.ASR) {
      throw new ViamError("Not implemented now");
    } else {
      throw new ViamError("Inverting builtin is not supported");
    }

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
