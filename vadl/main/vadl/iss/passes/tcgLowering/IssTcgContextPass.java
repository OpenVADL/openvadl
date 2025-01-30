package vadl.iss.passes.tcgLowering;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import vadl.configuration.IssConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Instruction;
import vadl.viam.Specification;

/**
 * This pass does nothing except for instantiating a TCG context and returns it a pass result.
 */
// TODO: Add it to the viam as extension instead (as soon as extensions are implemented).
public class IssTcgContextPass extends Pass {
  
  /**
   * The pass result that holds a tcg context for every instruction.
   */
  public record Result(
      Map<Instruction, TcgCtx> tcgCtxs
  ) {
  }

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
  public Result execute(PassResults passResults, Specification viam)
      throws IOException {

    var targetSize = configuration().targetSize();

    var result = new Result(new HashMap<>());

    // Process each instruction in the ISA
    viam.isa().ifPresent(isa -> isa.ownInstructions()
        .forEach(instr -> {
              // Create context
              var ctx = new TcgCtx(instr.behavior(), targetSize);
              // Store the context
              result.tcgCtxs.put(instr, ctx);
            }
        ));

    return result;
  }
}