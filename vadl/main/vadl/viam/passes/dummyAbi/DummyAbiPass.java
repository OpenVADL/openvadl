package vadl.viam.passes.dummyAbi;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.Pair;
import vadl.utils.SourceLocation;
import vadl.viam.Identifier;
import vadl.viam.RegisterFile;
import vadl.viam.Specification;

/**
 * Inserts a {@link DummyAbi} to the {@link Specification}.
 */
public class DummyAbiPass extends Pass {

  public DummyAbiPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("DummyAbiPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam)
      throws IOException {
    var registerFile = viam.registerFiles().findFirst().get();
    var callerSaved = getCallerSaved(registerFile);
    var calleeSaved = getCalleeSaved(registerFile);
    var aliases = getAliases(registerFile);
    var argumentRegisters = getArgumentRegisters(registerFile);
    var returnRegisters = getReturnRegisters(registerFile);

    viam.add(new DummyAbi(new Identifier("dummyAbi", SourceLocation.INVALID_SOURCE_LOCATION),
        new DummyAbi.RegisterRef(registerFile, 1, DummyAbi.Alignment.WORD),
        new DummyAbi.RegisterRef(registerFile, 2, DummyAbi.Alignment.HALF_WORD),
        new DummyAbi.RegisterRef(registerFile, 8, DummyAbi.Alignment.WORD),
        aliases,
        callerSaved,
        calleeSaved,
        argumentRegisters,
        returnRegisters));

    return null;
  }

  private List<DummyAbi.RegisterRef> getReturnRegisters(RegisterFile registerFile) {
    return List.of(
        new DummyAbi.RegisterRef(registerFile, 10, DummyAbi.Alignment.NO_ALIGNMENT),
        new DummyAbi.RegisterRef(registerFile, 11, DummyAbi.Alignment.NO_ALIGNMENT)
    );
  }

  private List<DummyAbi.RegisterRef> getArgumentRegisters(RegisterFile registerFile) {
    return List.of(
        new DummyAbi.RegisterRef(registerFile, 10, DummyAbi.Alignment.NO_ALIGNMENT),
        new DummyAbi.RegisterRef(registerFile, 11, DummyAbi.Alignment.NO_ALIGNMENT),
        new DummyAbi.RegisterRef(registerFile, 12, DummyAbi.Alignment.NO_ALIGNMENT),
        new DummyAbi.RegisterRef(registerFile, 13, DummyAbi.Alignment.NO_ALIGNMENT),
        new DummyAbi.RegisterRef(registerFile, 14, DummyAbi.Alignment.NO_ALIGNMENT),
        new DummyAbi.RegisterRef(registerFile, 15, DummyAbi.Alignment.NO_ALIGNMENT),
        new DummyAbi.RegisterRef(registerFile, 16, DummyAbi.Alignment.NO_ALIGNMENT),
        new DummyAbi.RegisterRef(registerFile, 17, DummyAbi.Alignment.NO_ALIGNMENT)
    );
  }

  private Map<Pair<RegisterFile, Integer>, DummyAbi.RegisterAlias> getAliases(
      RegisterFile registerFile) {
    return Map.of(
        Pair.of(registerFile, 0), new DummyAbi.RegisterAlias("zero"),
        Pair.of(registerFile, 1), new DummyAbi.RegisterAlias("ra"),
        Pair.of(registerFile, 2), new DummyAbi.RegisterAlias("sp"),
        Pair.of(registerFile, 3), new DummyAbi.RegisterAlias("gp"),
        Pair.of(registerFile, 4), new DummyAbi.RegisterAlias("tp"),
        Pair.of(registerFile, 8), new DummyAbi.RegisterAlias("fp")
    );
  }

  private List<DummyAbi.RegisterRef> getCalleeSaved(RegisterFile registerFile) {
    return List.of(
        new DummyAbi.RegisterRef(registerFile, 2, DummyAbi.Alignment.NO_ALIGNMENT),
        new DummyAbi.RegisterRef(registerFile, 8, DummyAbi.Alignment.NO_ALIGNMENT),
        new DummyAbi.RegisterRef(registerFile, 9, DummyAbi.Alignment.NO_ALIGNMENT),
        new DummyAbi.RegisterRef(registerFile, 18, DummyAbi.Alignment.NO_ALIGNMENT),
        new DummyAbi.RegisterRef(registerFile, 19, DummyAbi.Alignment.NO_ALIGNMENT),
        new DummyAbi.RegisterRef(registerFile, 20, DummyAbi.Alignment.NO_ALIGNMENT),
        new DummyAbi.RegisterRef(registerFile, 21, DummyAbi.Alignment.NO_ALIGNMENT),
        new DummyAbi.RegisterRef(registerFile, 22, DummyAbi.Alignment.NO_ALIGNMENT),
        new DummyAbi.RegisterRef(registerFile, 23, DummyAbi.Alignment.NO_ALIGNMENT),
        new DummyAbi.RegisterRef(registerFile, 24, DummyAbi.Alignment.NO_ALIGNMENT),
        new DummyAbi.RegisterRef(registerFile, 25, DummyAbi.Alignment.NO_ALIGNMENT),
        new DummyAbi.RegisterRef(registerFile, 26, DummyAbi.Alignment.NO_ALIGNMENT),
        new DummyAbi.RegisterRef(registerFile, 27, DummyAbi.Alignment.NO_ALIGNMENT)
    );
  }

  private List<DummyAbi.RegisterRef> getCallerSaved(RegisterFile registerFile) {
    return List.of(
        new DummyAbi.RegisterRef(registerFile, 1, DummyAbi.Alignment.NO_ALIGNMENT),
        new DummyAbi.RegisterRef(registerFile, 10, DummyAbi.Alignment.NO_ALIGNMENT),
        new DummyAbi.RegisterRef(registerFile, 11, DummyAbi.Alignment.NO_ALIGNMENT),
        new DummyAbi.RegisterRef(registerFile, 12, DummyAbi.Alignment.NO_ALIGNMENT),
        new DummyAbi.RegisterRef(registerFile, 13, DummyAbi.Alignment.NO_ALIGNMENT),
        new DummyAbi.RegisterRef(registerFile, 14, DummyAbi.Alignment.NO_ALIGNMENT),
        new DummyAbi.RegisterRef(registerFile, 15, DummyAbi.Alignment.NO_ALIGNMENT),
        new DummyAbi.RegisterRef(registerFile, 16, DummyAbi.Alignment.NO_ALIGNMENT),
        new DummyAbi.RegisterRef(registerFile, 17, DummyAbi.Alignment.NO_ALIGNMENT),
        new DummyAbi.RegisterRef(registerFile, 5, DummyAbi.Alignment.NO_ALIGNMENT),
        new DummyAbi.RegisterRef(registerFile, 6, DummyAbi.Alignment.NO_ALIGNMENT),
        new DummyAbi.RegisterRef(registerFile, 7, DummyAbi.Alignment.NO_ALIGNMENT),
        new DummyAbi.RegisterRef(registerFile, 28, DummyAbi.Alignment.NO_ALIGNMENT),
        new DummyAbi.RegisterRef(registerFile, 29, DummyAbi.Alignment.NO_ALIGNMENT),
        new DummyAbi.RegisterRef(registerFile, 30, DummyAbi.Alignment.NO_ALIGNMENT),
        new DummyAbi.RegisterRef(registerFile, 31, DummyAbi.Alignment.NO_ALIGNMENT)
    );
  }
}
