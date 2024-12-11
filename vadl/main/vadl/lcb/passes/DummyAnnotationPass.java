package vadl.lcb.passes;

import java.io.IOException;
import java.util.HashSet;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * Dummy pass to add the {@link EncodeAssemblyImmediateAnnotation} to the instructions s.t.
 * they get printed correctly.
 */
public class DummyAnnotationPass extends Pass {
  public DummyAnnotationPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("DummyAnnotationPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var set = new HashSet<String>();
    set.add("AUIPC");
    set.add("BEQ");
    set.add("BGE");
    set.add("BGEU");
    set.add("BLT");
    set.add("BLTU");
    set.add("BNE");
    set.add("JAL");
    set.add("LUI");
    viam.isa().map(isa -> isa.ownInstructions().stream())
        .orElseGet(Stream::empty)
        .forEach(instruction -> {
          if (set.contains(instruction.identifier.simpleName())) {
            instruction.assembly().addAnnotation(new EncodeAssemblyImmediateAnnotation());
          }
        });

    return null;
  }
}
