package vadl.cppCodeGen.common;

import vadl.cppCodeGen.AbstractRelocationCodeGenerator;
import vadl.cppCodeGen.CppTypeMap;
import vadl.cppCodeGen.context.CGenContext;
import vadl.cppCodeGen.context.CNodeContext;
import vadl.cppCodeGen.mixins.CRelocationMixins;
import vadl.cppCodeGen.model.GcbUpdateFieldRelocationCppFunction;
import vadl.javaannotations.DispatchFor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FuncParamNode;

/**
 * Produce a pure function that generates relocations.
 */
@DispatchFor(
    value = ExpressionNode.class,
    context = CNodeContext.class,
    include = "vadl.cppCodeGen.model.nodes"
)
public class UpdateFieldRelocationFunctionCodeGenerator extends AbstractRelocationCodeGenerator
    implements CRelocationMixins {
  protected final String functionName;
  protected final CNodeContext context;

  /**
   * Creates a new pure function code generator for the specified function.
   */
  public UpdateFieldRelocationFunctionCodeGenerator(
      GcbUpdateFieldRelocationCppFunction gcbUpdateFieldRelocationCppFunction) {
    super(gcbUpdateFieldRelocationCppFunction);
    this.functionName = function.identifier.lower();
    this.context = new CNodeContext(
        builder::append,
        (ctx, node)
            -> UpdateFieldRelocationFunctionCodeGeneratorDispatcher.dispatch(this, ctx,
            (ExpressionNode) node)
    );
  }

  @Override
  public void handle(CGenContext<Node> ctx, FuncParamNode toHandle) {
    ctx.wr(toHandle.parameter().simpleName());
  }

  @Override
  public String genFunctionSignature() {
    var returnType = function.returnType().asDataType().fittingCppType();

    function.ensure(returnType != null, "No fitting Cpp type found for return type %s", returnType);
    function.ensure(function.behavior().isPureFunction(), "Function is not pure.");

    return CppTypeMap.getCppTypeNameByVadlType(returnType)
        + " %s(%s)".formatted(functionName, genFunctionParameters(function.parameters()));
  }

  @Override
  public CNodeContext context() {
    return context;
  }

  @Override
  public String genFunctionName() {
    return functionName;
  }
}
