package vadl.cppCodeGen.mixins;

import java.util.stream.Collectors;
import vadl.cppCodeGen.context.CGenContext;
import vadl.cppCodeGen.model.nodes.CppUpdateBitRangeNode;
import vadl.javaannotations.Handler;
import vadl.types.BitsType;
import vadl.viam.graph.Node;

/**
 * Code generation mixins for relocations.
 */
public interface CRelocationMixins extends CDefaultMixins.Utils {
  /**
   * Generate code for {@link CppUpdateBitRangeNode}.
   */
  @Handler
  default void handle(CGenContext<Node> ctx, CppUpdateBitRangeNode toHandle) {
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
}
