package vadl.gcb.passes.relocation;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import vadl.cppCodeGen.model.CppFunction;
import vadl.cppCodeGen.model.CppUpdateBitRangeNode;
import vadl.types.BuiltInTable;
import vadl.utils.SourceLocation;
import vadl.viam.Constant;
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.Identifier;
import vadl.viam.Parameter;
import vadl.viam.graph.Graph;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.FuncParamNode;

/**
 * A relocation is a function where we have an old value, we know the format and have a new value.
 * This class will generate a {@link Function} which changes the old value (=immediate) in the
 * format with the new value.
 */
public class BitMaskFunctionGenerator {
  public static String generateFunctionName(Format format, Format.Field field) {
    return format.identifier.lower() + "_" + field.identifier.simpleName();
  }

  /**
   * Generates a {@link Function} which updates the value with the given {@code field}.
   *
   * @param format which the relocation updates.
   * @param field  which should be updated. It must be a field of the {@code format}.
   * @return a {@link Function} which updates the value.
   */
  public static CppFunction generateUpdateFunction(Format format, Format.Field field) {
    var parameterInstWord =
        new Parameter(new Identifier("instWord", SourceLocation.INVALID_SOURCE_LOCATION),
            format.type());
    var parameterNewValue =
        new Parameter(new Identifier("newValue", SourceLocation.INVALID_SOURCE_LOCATION),
            format.type());

    return new CppFunction(
        new Identifier(generateFunctionName(format, field), SourceLocation.INVALID_SOURCE_LOCATION),
        new Parameter[] {parameterInstWord, parameterNewValue},
        format.type(),
        getBehavior(format, field, parameterInstWord, parameterNewValue));
  }

  private static Graph getBehavior(Format format,
                                   Format.Field field,
                                   Parameter parameterInstWord,
                                   Parameter parameterNewValue) {
    var graph = new Graph("updatingValue");
    var ty = format.type();

    /*

    var otherFields =
        Arrays.stream(format.fields()).filter(x -> x != field).map(Format.Field::bitSlice).toList();
    var instrWordBitMask =
        new ConstantNode(
            Constant.Value.of(generateBitMaskForInstrWord(otherFields).longValue(), ty));
    var newValueBitMask =
        new ConstantNode(Constant.Value.of(generateBitMask(field.size()), ty));
    var fieldNode = new BuiltInCall(BuiltInTable.AND,
        new NodeList<>(new FuncParamNode(parameterNewValue),
            newValueBitMask),
        ty);

    var lowestLsb = Arrays.stream(format.fields()).filter(x -> x == field)
        .map(Format.Field::bitSlice)
        .flatMap(Constant.BitSlice::parts)
        .mapToInt(Constant.BitSlice.Part::lsb)
        .min()
        .getAsInt();

    var orNode = new BuiltInCall(
        BuiltInTable.OR,
        new NodeList<>(new BuiltInCall(BuiltInTable.AND,
            new NodeList<>(new FuncParamNode(parameterInstWord), instrWordBitMask), ty),
            new BuiltInCall(BuiltInTable.LSL,
                new NodeList<>(
                    fieldNode,
                    new ConstantNode(Constant.Value.of(lowestLsb, ty))
                ),
                ty)),
        ty
    );
     */

    var node = new ReturnNode(new CppUpdateBitRangeNode(
        ty,
        new FuncParamNode(parameterInstWord),
        new FuncParamNode(parameterNewValue),
        field
    ));
    graph.addWithInputs(new StartNode(node));
    graph.addWithInputs(node);

    return graph;
  }

  // TODO: @kper remove this?
  @SuppressWarnings("unused")
  private static BigInteger generateBitMaskForInstrWord(List<Constant.BitSlice> slices) {
    var x = BigInteger.ZERO;

    for (var slice : slices) {
      for (var part : slice.parts().toList()) {
        for (var i = part.lsb(); i <= part.msb(); i++) {
          x = x.setBit(1);
        }
      }
    }

    return x;
  }

  // TODO: @kper remove this?
  @SuppressWarnings("unused")
  private static long generateBitMask(int size) {
    return (1L << size) - 1;
  }
}
