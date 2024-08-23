package vadl.lcb.passes.dummyAbi;

import java.io.IOException;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import vadl.pass.Pass;
import vadl.pass.PassKey;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.SourceLocation;
import vadl.viam.Identifier;
import vadl.viam.Specification;

/**
 * Inserts a {@link DummyAbi} to the {@link Specification}.
 */
public class DummyAbiPass extends Pass {

  @Override
  public PassName getName() {
    return new PassName("DummyAbiPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam)
      throws IOException {
    var registerFile = viam.registerFiles().findFirst().get();
    viam.add(new DummyAbi(new Identifier("dummyAbi", SourceLocation.INVALID_SOURCE_LOCATION),
        new DummyAbi.RegisterRef(registerFile, 1, DummyAbi.Alignment.WORD),
        new DummyAbi.RegisterRef(registerFile, 2, DummyAbi.Alignment.HALF_WORD),
        new DummyAbi.RegisterRef(registerFile, 8, DummyAbi.Alignment.WORD)));

    return null;
  }
}
