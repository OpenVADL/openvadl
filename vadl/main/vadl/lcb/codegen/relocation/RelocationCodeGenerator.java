package vadl.lcb.codegen.relocation;

import vadl.cppCodeGen.FunctionCodeGenerator;
import vadl.cppCodeGen.context.CGenContext;
import vadl.types.BitsType;
import vadl.viam.Function;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.ReadRegNode;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.ZeroExtendNode;

public class RelocationCodeGenerator extends FunctionCodeGenerator {
  /**
   * Creates a new code generator for the specified function.
   *
   * @param function the function for which code should be generated
   */
  public RelocationCodeGenerator(Function function) {
    super(function);
  }

  @Override
  protected void handle(CGenContext<Node> ctx, ReadRegNode toHandle) {

  }

  @Override
  protected void handle(CGenContext<Node> ctx, ReadRegFileNode toHandle) {

  }

  @Override
  protected void handle(CGenContext<Node> ctx, ReadMemNode toHandle) {

  }

  @Override
  protected void handle(CGenContext<Node> ctx, FieldAccessRefNode toHandle) {

  }

  @Override
  protected void handle(CGenContext<Node> ctx, FieldRefNode toHandle) {

  }

  @Override
  protected void handle(CGenContext<Node> ctx, ConstantNode toHandle) {

  }

  @Override
  protected void handle(CGenContext<Node> ctx, ZeroExtendNode toHandle) {

  }

  @Override
  protected void handle(CGenContext<Node> ctx, SliceNode toHandle) {
    var parts = toHandle.bitSlice().parts().toList();
    ctx.wr("(");

    int acc = 0;
    for (int i = parts.size() - 1; i >= 0; i--) {
      if (i != parts.size() - 1) {
        ctx.wr(" | ");
      }

      var part = parts.get(i);
      var bitWidth = ((BitsType) toHandle.value().type()).bitWidth();
        ctx.wr(
            String.format("project_range<%d, %d>(std::bitset<%d>(", part.lsb(), part.msb(),
                bitWidth));
         handle(toHandle.value());
        ctx.wr(String.format(")) << %d", acc));

      acc += part.msb() - part.lsb() + 1;
    }
    ctx.wr(").to_ulong()");
  }
}
