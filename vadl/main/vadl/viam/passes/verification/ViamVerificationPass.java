package vadl.viam.passes.verification;

import java.io.IOException;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.configuration.GeneralConfiguration;
import vadl.dump.HtmlDumpPass;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Definition;
import vadl.viam.Specification;
import vadl.viam.ViamError;

/**
 * A pass that runs the {@link ViamVerifier} on the given VIAM specification.
 * It calls the {@link Definition#verify()} method on each definition in the specification.
 * This pass will fail if some invalid state was detected.
 *
 * @see ViamVerifier
 */
public class ViamVerificationPass extends Pass {
  private static final Logger log = LoggerFactory.getLogger(ViamVerificationPass.class);

  public ViamVerificationPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("ViamVerificationPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam)
        throws IOException {
    try {
      ViamVerifier.verifyAllIn(viam);
    } catch (ViamError e) {
      var result = (HtmlDumpPass.Result) new HtmlDumpPass(
            HtmlDumpPass.Config.from(configuration(),
                  getName().value() + " Exception",
                  "This dump is due to an exception while running the viam verification pass.")
      ).execute(passResults, viam);
      log.error("A verification error was found.\nSee the dump at {}\n\n",
            result.emittedFile.toAbsolutePath());
      throw e;
    }
    return null;
  }
}
