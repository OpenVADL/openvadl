package vadl.lcb.passes.isaMatching;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import vadl.configuration.LcbConfiguration;
import vadl.gcb.passes.IsaMatchingUtils;
import vadl.gcb.valuetypes.RelocationCtx;
import vadl.gcb.valuetypes.RelocationFunctionLabel;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BuiltInTable;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Relocation;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.TruncateNode;

/**
 * A {@link InstructionSetArchitecture} contains a {@link List} of {@link Relocation}.
 * This pass has the task to classify a relocation into LO, HI or unknown.
 */
public class IsaRelocationMatchingPass extends Pass implements IsaMatchingUtils {
  public IsaRelocationMatchingPass(LcbConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("IsaRelocationMatchingPass");
  }

  /**
   * Result of the pass.
   */
  public record Result(Map<RelocationFunctionLabel, List<Relocation>> labels) {

  }

  @Nullable
  @Override
  public Result execute(PassResults passResults,
                        Specification viam)
      throws IOException {
    var isa = viam.isa().orElse(null);
    if (isa == null) {
      return new Result(Collections.emptyMap());
    }

    isa.ownRelocations().forEach(relocation -> {
      if (findHi(relocation)) {
        relocation.attachExtension(new RelocationCtx(RelocationFunctionLabel.HI));
      } else if (findLo(relocation)) {
        relocation.attachExtension(new RelocationCtx(RelocationFunctionLabel.LO));
      } else {
        relocation.attachExtension(new RelocationCtx(RelocationFunctionLabel.UNKNOWN));
      }
    });

    var labels = createRelocationFunctionLabelMap(viam);
    return new Result(labels);
  }

  private boolean findHi(Relocation relocation) {
    // Check whether there is a shift and a truncation.
    return relocation.behavior().getNodes(BuiltInCall.class)
        .anyMatch(x -> x.builtIn() == BuiltInTable.LSR)
        && relocation.behavior().getNodes(TruncateNode.class).findAny().isPresent()
        && relocation.isAbsolute();
  }

  private boolean findLo(Relocation relocation) {
    // Check whether there is no operation in the relocation but only a truncation.
    return relocation.behavior().getNodes(BuiltInCall.class).toList().isEmpty()
        && relocation.behavior().getNodes(TruncateNode.class).findAny().isPresent()
        && relocation.isAbsolute();
  }
}
