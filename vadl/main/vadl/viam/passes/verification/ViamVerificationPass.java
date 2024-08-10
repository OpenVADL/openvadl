package vadl.viam.passes.verification;

import java.io.IOException;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import vadl.pass.Pass;
import vadl.pass.PassKey;
import vadl.pass.PassName;
import vadl.viam.Definition;
import vadl.viam.Specification;

/**
 * A pass that runs the {@link ViamVerifier} on the given VIAM specification.
 * It calls the {@link Definition#verify()} method on each definition in the specification.
 * This pass will fail if some invalid state was detected.
 *
 * @see ViamVerifier
 */
public class ViamVerificationPass extends Pass {
  @Override
  public PassName getName() {
    return new PassName("ViamVerificationPass");
  }

  @Nullable
  @Override
  public Object execute(Map<PassKey, Object> passResults, Specification viam)
      throws IOException {
    ViamVerifier.verifyAllIn(viam);
    return null;
  }
}
