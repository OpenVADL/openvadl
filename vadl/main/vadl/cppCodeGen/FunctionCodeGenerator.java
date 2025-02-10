package vadl.cppCodeGen;

import static vadl.utils.GraphUtils.getSingleNode;

import vadl.cppCodeGen.context.CGenContext;
import vadl.cppCodeGen.context.CNodeContext;
import vadl.cppCodeGen.mixins.CDefaultMixins;
import vadl.javaannotations.DispatchFor;
import vadl.javaannotations.Handler;
import vadl.viam.Function;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.AsmBuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.ReadRegNode;
import vadl.viam.graph.dependency.SliceNode;

/**
 * Abstract base class responsible for generating C code from a given function's expression nodes.
 * Uses dispatching mechanisms to handle various node types
 * and produce a finalized C++ function.
 */
@DispatchFor(
    value = ExpressionNode.class,
    context = CNodeContext.class,
    include = "vadl.viam"
)
public abstract class FunctionCodeGenerator extends AbstractFunctionCodeGenerator
    implements CDefaultMixins.AllExpressions, CDefaultMixins.Utils {
  protected final CNodeContext context;

  /**
   * Creates a new code generator for the specified function.
   *
   * @param function the function for which code should be generated
   */
  public FunctionCodeGenerator(Function function) {
    super(function);
    this.context = new CNodeContext(
        builder::append,
        (ctx, node)
            -> FunctionCodeGeneratorDispatcher.dispatch(this, ctx, (ExpressionNode) node)
    );
  }

  @Handler
  protected abstract void handle(CGenContext<Node> ctx, ReadRegNode toHandle);

  @Handler
  protected abstract void handle(CGenContext<Node> ctx, ReadRegFileNode toHandle);

  @Handler
  protected abstract void handle(CGenContext<Node> ctx, ReadMemNode toHandle);

  @Handler
  protected abstract void handle(CGenContext<Node> ctx, FieldAccessRefNode toHandle);

  @Handler
  protected abstract void handle(CGenContext<Node> ctx, FieldRefNode toHandle);

  @Handler
  protected abstract void handle(CGenContext<Node> ctx, AsmBuiltInCall toHandle);

  @Override
  @Handler
  public abstract void handle(CGenContext<Node> ctx, SliceNode toHandle);

  public String genReturnExpression() {
    var returnNode = getSingleNode(function.behavior(), ReturnNode.class);
    return context.genToString(returnNode.value());
  }

  @Override
  public CNodeContext context() {
    return context;
  }
}


