package vadl.cppCodeGen.common;

import vadl.cppCodeGen.CppTypeMap;
import vadl.cppCodeGen.context.CGenContext;
import vadl.cppCodeGen.model.GcbFieldAccessCppFunction;
import vadl.cppCodeGen.model.GcbImmediateExtractionCppFunction;
import vadl.types.BitsType;
import vadl.viam.Format;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.SliceNode;

/**
 * Produce a pure function that allows to access format field references.
 * It generates code for accessing fields or extracting fields from instructions.
 */
public class GcbAccessOrExtractionFunctionCodeGenerator extends AccessFunctionCodeGenerator {

  /**
   * Constructor.
   */
  public GcbAccessOrExtractionFunctionCodeGenerator(GcbFieldAccessCppFunction accessFunction,
                                                    Format.FieldAccess fieldAccess,
                                                    String functionName,
                                                    String fieldName) {
    super(accessFunction, fieldAccess, functionName, fieldName);
  }

  /**
   * Constructor.
   */
  public GcbAccessOrExtractionFunctionCodeGenerator(GcbImmediateExtractionCppFunction extractionFunction,
                                                    Format.FieldAccess fieldAccess,
                                                    String functionName,
                                                    String fieldName) {
    super(extractionFunction, fieldAccess, functionName, fieldName);
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
  public String genFunctionName() {
    return functionName;
  }

  @Override
  public String genFunctionSignature() {
    var returnType = function.returnType().asDataType().fittingCppType();

    function.ensure(returnType != null, "No fitting Cpp type found for return type %s", returnType);
    function.ensure(function.behavior().isPureFunction(), "Function is not pure.");

    return CppTypeMap.getCppTypeNameByVadlType(returnType)
        + " %s(%s)".formatted(functionName, genFunctionParameters(function.parameters()));
  }
}
