package vadl.cppCodeGen.common;

import static vadl.error.DiagUtils.throwNotAllowed;

import vadl.cppCodeGen.FunctionCodeGenerator;
import vadl.cppCodeGen.context.CGenContext;
import vadl.error.Diagnostic;
import vadl.types.BuiltInTable;
import vadl.types.StringType;
import vadl.viam.Function;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.AsmBuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
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
  public void handle(CGenContext<Node> ctx, ConstantNode toHandle) {
    if (toHandle.type() == StringType.string()) {
      ctx.wr("\"%s\"", toHandle.constant().toString());
    } else {
      ctx.wr(toHandle.constant().asVal().decimal());
    }
  }

  @Override
  public void handle(CGenContext<Node> ctx, AsmBuiltInCall toHandle) {
    if (toHandle.asmBuiltIn() == BuiltInTable.LA_ID_EQ) {
      ctx.wr("VADL_asmparser_laideq(");
      toHandle.arguments().forEach(arg -> {
        handle(ctx, (ConstantNode) arg);
        if (arg != toHandle.arguments().get(toHandle.arguments().size() - 1)) {
          ctx.wr(",");
        }
      });
      ctx.wr(")");
    } else if (toHandle.asmBuiltIn() == BuiltInTable.LA_ID_IN) {
      ctx.wr("VADL_asmparser_laidin(");
      var lookaheadArg = (ConstantNode) toHandle.arguments().get(0);
      ctx.wr(lookaheadArg.constant().asVal().decimal());

      // create vector from strings arguments
      ctx.wr(", std::vector<std::string>{");
      toHandle.arguments().stream().skip(1).forEach(
          arg -> {
            handle(ctx, (ConstantNode) arg);
            if (arg != toHandle.arguments().get(toHandle.arguments().size() - 1)) {
              ctx.wr(",");
            }
          }
      );
      ctx.wr("})");
    } else {
      throw Diagnostic.error("Unknown AsmBuiltin.", toHandle.sourceLocation()).build();
    }
  }
}
