package vadl.cppCodeGen.common;

import static vadl.error.DiagUtils.throwNotAllowed;

import vadl.cppCodeGen.FunctionCodeGenerator;
import vadl.cppCodeGen.context.CGenContext;
import vadl.viam.Function;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncCallNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.ReadRegNode;
import vadl.viam.graph.dependency.ZeroExtendNode;

/**
 * Produce a pure function that does not access any entities except parameters.
 */
public class PureFunctionCodeGenerator extends FunctionCodeGenerator {

  /**
   * Creates a new pure function code generator for the specified function.
   *
   * @param function the function for which code should be generated
   */
  public PureFunctionCodeGenerator(Function function) {
    super(function);
  }

  @Override
  protected void handle(CGenContext<Node> ctx, ReadRegNode toHandle) {
    throwNotAllowed(toHandle, "Register reads");
  }

  @Override
  protected void handle(CGenContext<Node> ctx, ReadRegFileNode toHandle) {
    throwNotAllowed(toHandle, "Register reads");
  }

  @Override
  protected void handle(CGenContext<Node> ctx, ReadMemNode toHandle) {
    throwNotAllowed(toHandle, "Memory reads");
  }

  @Override
  protected void handle(CGenContext<Node> ctx, FieldAccessRefNode toHandle) {
    throwNotAllowed(toHandle, "Format field accesses");
  }

  @Override
  public void handle(CGenContext<Node> ctx, FieldRefNode toHandle) {
    throwNotAllowed(toHandle, "Format field accesses");
  }
}
