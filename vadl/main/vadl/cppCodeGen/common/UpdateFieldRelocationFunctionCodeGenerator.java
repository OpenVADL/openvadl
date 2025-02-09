package vadl.cppCodeGen.common;

import static vadl.utils.GraphUtils.getSingleNode;

import java.util.stream.Collectors;
import vadl.cppCodeGen.AbstractFunctionCodeGenerator;
import vadl.cppCodeGen.CppTypeMap;
import vadl.cppCodeGen.context.CGenContext;
import vadl.cppCodeGen.context.CNodeContext;
import vadl.cppCodeGen.model.GcbUpdateFieldRelocationCppFunction;
import vadl.cppCodeGen.model.nodes.CppUpdateBitRangeNode;
import vadl.javaannotations.DispatchFor;
import vadl.javaannotations.Handler;
import vadl.types.BitsType;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Produce a pure function that generates relocations.
 */
@DispatchFor(
    value = ExpressionNode.class,
    context = CNodeContext.class,
    include = "vadl.cppCodeGen.model.nodes"
)
public class UpdateFieldRelocationFunctionCodeGenerator extends AbstractFunctionCodeGenerator {
  protected final String functionName;
  protected final CNodeContext context;

  /**
   * Creates a new pure function code generator for the specified function.
   */
  public UpdateFieldRelocationFunctionCodeGenerator(
      GcbUpdateFieldRelocationCppFunction gcbUpdateFieldRelocationCppFunction) {
    super(gcbUpdateFieldRelocationCppFunction);
    this.functionName = function.simpleName();
    this.context = new CNodeContext(
        builder::append,
        (ctx, node)
            -> UpdateFieldRelocationFunctionCodeGeneratorDispatcher.dispatch(this, ctx,
            (ExpressionNode) node)
    );
  }

  @Handler
  public void handle(CGenContext<Node> ctx, CppUpdateBitRangeNode toHandle) {
    var bitWidth = ((BitsType) toHandle.type()).bitWidth();
    ctx.wr("set_bits(");

    // Inst
    ctx.wr(String.format("std::bitset<%d>(", bitWidth));
    ctx.gen(toHandle.value);
    ctx.wr("), ");

    // New value
    ctx.wr(String.format("std::bitset<%d>(", bitWidth));
    ctx.gen(toHandle.patch);
    ctx.wr(")");

    // Parts
    ctx.wr(", std::vector<int> { ");
    ctx.wr(toHandle.field.bitSlice()
        .stream()
        .mapToObj(String::valueOf)
        .collect(Collectors.joining(", ")));
    ctx.wr(" } ");
    ctx.wr(").to_ulong()");
  }

  @Override
  public String genFunctionSignature() {
    var returnType = function.returnType().asDataType().fittingCppType();

    function.ensure(returnType != null, "No fitting Cpp type found for return type %s", returnType);
    function.ensure(function.behavior().isPureFunction(), "Function is not pure.");

    return CppTypeMap.getCppTypeNameByVadlType(returnType)
        + " %s(%s)".formatted(functionName, "");
  }

  public String genReturnExpression() {
    var returnNode = getSingleNode(function.behavior(), ReturnNode.class);
    return context.genToString(returnNode.value());
  }

  @Override
  public CNodeContext context() {
    return context;
  }
}
