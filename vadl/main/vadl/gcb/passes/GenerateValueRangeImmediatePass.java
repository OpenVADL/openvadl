package vadl.gcb.passes;

import java.io.IOException;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BitsType;
import vadl.viam.Format;
import vadl.viam.Format.FieldAccess;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.FieldAccessRefNode;

public class GenerateValueRangeImmediatePass extends Pass {
  public GenerateValueRangeImmediatePass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("GenerateValueRangeImmediatePass");
  }

  /**
   * Get the lowest possible value that the immediate with the given {@code formatBitSize} can
   * have.
   */
  public static int lowestPossibleValue(int formatBitSize, BitsType rawType) {
    return rawType.isSigned()
        ? (int) (-1 * Math.pow(2, (double) formatBitSize - 1))
        : 0;
  }

  /**
   * Get the highest possible value that the immediate with the given {@code formatBitSize} can
   * have.
   */
  public static int highestPossibleValue(int formatBitSize, BitsType rawType) {
    return
        (int) (rawType.isSigned()
            ? Math.pow(2, (double) formatBitSize - 1)
            : Math.pow(2, formatBitSize)) - 1;
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var fieldResult =
        (IdentifyFieldUsagePass.ImmediateDetectionContainer) passResults
            .lastResultOf(IdentifyFieldUsagePass.class);

    viam.isa().map(x -> x.ownInstructions().stream())
        .orElse(Stream.empty())
        .forEach(instruction -> {
          var fields = fieldResult.getImmediates(instruction.format());
          var ctx = new ValueRangeCtx();

          fields.forEach(field -> {
            var ty = getType(instruction, field);
            var lowest = lowestPossibleValue(field.size(), ty);
            var highest = highestPossibleValue(field.size(), ty);
            var range = new ValueRange(lowest, highest);
            ctx.add(field, range);
          });

          instruction.attachExtension(ctx);
        });

    return null;
  }

  /**
   * The fields always are unsigned. To know whether the immediate is going to be
   * unsigned or signed, we have to check whether there exist a {@link FieldAccess}.
   * If it does, then take its type. If not then just use the field's type.
   */
  private BitsType getType(Instruction instruction, Format.Field field) {
    var accessType = instruction.behavior().getNodes(FieldAccessRefNode.class)
        .filter(fieldAccessRefNode -> fieldAccessRefNode.fieldAccess().fieldRef() == field)
        .findFirst()
        .map(x -> (BitsType) x.fieldAccess().type());

    return accessType.orElseGet(() -> (BitsType) field.type());
  }
}
