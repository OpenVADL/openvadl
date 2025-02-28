package vadl.lcb.passes.relocation;

import java.io.IOException;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.gcb.passes.relocation.model.Modifier;
import vadl.gcb.passes.relocation.model.ModifierCtx;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Relocation;
import vadl.viam.Specification;

/**
 * Attaches the {@link Modifier} to the {@link Relocation}.
 */
public class GenerateModifiersPass extends Pass {
  public GenerateModifiersPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("GenerateModifiersPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var relocations =
        viam.isa().map(isa -> isa.ownRelocations().stream()).orElseGet(Stream::empty).toList();

    for (var relocation : relocations) {
      var abs = new Modifier("MO_ABS_" + relocation.simpleName());
      var rel = new Modifier("MO_REL_" + relocation.simpleName());
      var ctx = new ModifierCtx(abs, rel);
      relocation.attachExtension(ctx);
    }

    return null;
  }
}
