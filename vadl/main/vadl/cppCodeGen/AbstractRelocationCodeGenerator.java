package vadl.cppCodeGen;

import vadl.cppCodeGen.common.UpdateFieldRelocationFunctionCodeGenerator;
import vadl.cppCodeGen.common.ValueRelocationFunctionCodeGenerator;
import vadl.cppCodeGen.context.CGenContext;
import vadl.cppCodeGen.mixins.CDefaultMixins;
import vadl.cppCodeGen.mixins.CRelocationMixins;
import vadl.types.BitsType;
import vadl.viam.Function;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.SliceNode;

/**
 * This class overrides the default implementation of {@link CDefaultMixins} for
 * generating code for {@link ValueRelocationFunctionCodeGenerator} and
 * {@link UpdateFieldRelocationFunctionCodeGenerator}. This class overrides only existing
 * implementations, while {@link CRelocationMixins} contains the code for custom CPP nodes.
 */
public abstract class AbstractRelocationCodeGenerator extends AbstractFunctionCodeGenerator {
  public AbstractRelocationCodeGenerator(Function function) {
    super(function);
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

  /**
   * Generate an expression which bitmasks a value. It will create "1"
   * for the given size.
   */
  private String generateBitmask(int size) {
    return String.format("((1UL << %d) - 1)", size);
  }

}
