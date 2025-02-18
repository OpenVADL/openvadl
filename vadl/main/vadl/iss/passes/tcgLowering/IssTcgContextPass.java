package vadl.iss.passes.tcgLowering;

import java.io.IOException;
import javax.annotation.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * This pass only attaches a new {@link TcgCtx} to each instruction as an extension.
 * The TCG context is information per instruction that is used by several passes.
 */
public class IssTcgContextPass extends Pass {

  public IssTcgContextPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("ISS TCG Context Pass");
  }

  @Override
  public IssConfiguration configuration() {
    return (IssConfiguration) super.configuration();
  }

  @Override
  @Nullable
  public Object execute(PassResults passResults, Specification viam)
      throws IOException {

    var targetSize = configuration().targetSize();

    // Process each instruction in the ISA
    viam.isa().ifPresent(isa -> isa.ownInstructions()
        // attach new TCG context to each instruction
        .forEach(instr -> instr.attachExtension(new TcgCtx(instr.behavior(), targetSize))
        ));

    return null;
  }
}