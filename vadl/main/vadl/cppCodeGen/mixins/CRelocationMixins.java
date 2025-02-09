package vadl.cppCodeGen.mixins;

import java.util.stream.Collectors;
import vadl.cppCodeGen.CppTypeMap;
import vadl.cppCodeGen.context.CGenContext;
import vadl.cppCodeGen.model.nodes.CppUpdateBitRangeNode;
import vadl.cppCodeGen.passes.typeNormalization.CppSignExtendNode;
import vadl.cppCodeGen.passes.typeNormalization.CppZeroExtendNode;
import vadl.javaannotations.Handler;
import vadl.types.BitsType;
import vadl.types.BoolType;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.ZeroExtendNode;

public interface CRelocationMixins extends CDefaultMixins.Utils {
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

  default void visit(CGenContext<Node> ctx, CppSignExtendNode toHandle) {
    ctx.gen((SignExtendNode) toHandle);

    if (toHandle.originalType() instanceof BitsType bitsType) {
      ctx.wr(" & " + generateBitmask(bitsType.bitWidth()));
    }
  }

  default void visit(CGenContext<Node> ctx, CppZeroExtendNode toHandle) {
    ctx.gen((ZeroExtendNode) toHandle);

    if (toHandle.originalType() instanceof BitsType bitsType) {
      ctx.wr(" & " + generateBitmask(bitsType.bitWidth()));
    }
  }

  default void visit(CGenContext<Node> ctx, TruncateNode toHandle) {
    if (toHandle.type() instanceof BoolType) {
      ctx.wr("((" + CppTypeMap.getCppTypeNameByVadlType(toHandle.type()) + ") ");
      ctx.gen(toHandle);
      ctx.wr(" & 0x1)");
    } else {
      ctx.wr("((" + CppTypeMap.getCppTypeNameByVadlType(toHandle.type()) + ") ");
      ctx.gen(toHandle);
      ctx.wr(")");
    }
  }

  default String generateBitmask(int size) {
    return String.format("((1UL << %d) - 1)", size);
  }
}
