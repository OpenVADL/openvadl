package vadl.cppCodeGen.common;

import static vadl.error.DiagUtils.throwNotAllowed;

import vadl.cppCodeGen.FunctionCodeGenerator;
import vadl.cppCodeGen.context.CGenContext;
import vadl.viam.Function;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.AsmBuiltInCall;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.ReadRegNode;
import vadl.viam.graph.dependency.SliceNode;

/**
 * Produce a function that can access special functions of the asm parser.
 */
public class AsmParserFunctionCodeGenerator extends FunctionCodeGenerator {
  /**
   * Creates a new asm parser function code generator for the specified function.
   *
   * @param function the function for which code should be generated
   */
  public AsmParserFunctionCodeGenerator(Function function) {
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

  @Override
  public void handle(CGenContext<Node> ctx, SliceNode toHandle) {
    throwNotAllowed(toHandle, "Slice node reads");
  }

  @Override
  public void handle(CGenContext<Node> ctx, AsmBuiltInCall toHandle) {
    // TODO: Call handwritten functions of RecursiveDescentParser
  }
}
