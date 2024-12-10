package vadl.viam.passes.dummyAbi;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.Pair;
import vadl.utils.SourceLocation;
import vadl.viam.Identifier;
import vadl.viam.PseudoInstruction;
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
    var regOpt = viam.registerFiles().findFirst();
    if (regOpt.isEmpty()) {
      // skip if no register file available
      return null;
    }

    var registerFile = regOpt.get();
    var callerSaved = getCallerSaved(registerFile);
    var calleeSaved = getCalleeSaved(registerFile);
    var aliases = getAliases(registerFile);
    var argumentRegisters = getArgumentRegisters(registerFile);
    var returnRegisters = getReturnRegisters(registerFile);
    var returnSequence = getReturnSequence(viam);
    var callSequence = getCallSequence(viam);
    var addressSequence = getAddressSequence(viam);

    viam.add(new DummyAbi(new Identifier("dummyAbi", SourceLocation.INVALID_SOURCE_LOCATION),
        new DummyAbi.RegisterRef(registerFile, 1, DummyAbi.Alignment.WORD),
        new DummyAbi.RegisterRef(registerFile, 2, DummyAbi.Alignment.HALF_WORD),
        new DummyAbi.RegisterRef(registerFile, 8, DummyAbi.Alignment.WORD),
        new DummyAbi.RegisterRef(registerFile, 3, DummyAbi.Alignment.WORD),
        aliases,
        callerSaved,
        calleeSaved,
        argumentRegisters,
        returnRegisters,
        returnSequence,
        callSequence,
        addressSequence));

    return null;
  }

  private PseudoInstruction getReturnSequence(Specification viam) {
    var retInstruction =
        viam.isa().map(isa -> isa.ownPseudoInstructions().stream()).orElseGet(Stream::empty)
            .filter(x -> x.identifier.simpleName().equals("RET"))
            .findFirst()
            .get();

    var x = new PseudoInstruction(
        new Identifier("RESERVED_PSEUDO_RET", retInstruction.identifier.sourceLocation()),
        retInstruction.parameters(),
        retInstruction.behavior().copy(),
        retInstruction.assembly());
    x.setSourceLocation(retInstruction.sourceLocation());
    return x;
  }

  private PseudoInstruction getAddressSequence(Specification viam) {
    return
        viam.isa().map(isa -> isa.ownPseudoInstructions().stream()).orElseGet(Stream::empty)
            .filter(x -> x.identifier.simpleName().equals("LLA"))
            .findFirst()
            .get();
  }

  private PseudoInstruction getCallSequence(Specification viam) {
    var callInstruction =
        viam.isa().map(isa -> isa.ownPseudoInstructions().stream()).orElseGet(Stream::empty)
            .filter(x -> x.identifier.simpleName().equals("CALL"))
            .findFirst()
            .get();

    var x = new PseudoInstruction(
        new Identifier("RESERVED_PSEUDO_CALL", callInstruction.identifier.sourceLocation()),
        callInstruction.parameters(),
        callInstruction.behavior().copy(),
        callInstruction.assembly());
    x.setSourceLocation(callInstruction.sourceLocation());
    return x;
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
    var map = new HashMap<Pair<RegisterFile, Integer>, DummyAbi.RegisterAlias>();
    map.put(Pair.of(registerFile, 0), new DummyAbi.RegisterAlias("zero"));
    map.put(Pair.of(registerFile, 1), new DummyAbi.RegisterAlias("ra"));
    map.put(Pair.of(registerFile, 2), new DummyAbi.RegisterAlias("sp"));
    map.put(Pair.of(registerFile, 3), new DummyAbi.RegisterAlias("gp"));
    map.put(Pair.of(registerFile, 4), new DummyAbi.RegisterAlias("tp"));
    map.put(Pair.of(registerFile, 8), new DummyAbi.RegisterAlias("fp"));
    map.put(Pair.of(registerFile, 9), new DummyAbi.RegisterAlias("s1"));
    map.put(Pair.of(registerFile, 10), new DummyAbi.RegisterAlias("a0"));
    map.put(Pair.of(registerFile, 11), new DummyAbi.RegisterAlias("a1"));
    map.put(Pair.of(registerFile, 12), new DummyAbi.RegisterAlias("a2"));
    map.put(Pair.of(registerFile, 13), new DummyAbi.RegisterAlias("a3"));
    map.put(Pair.of(registerFile, 14), new DummyAbi.RegisterAlias("a4"));
    map.put(Pair.of(registerFile, 15), new DummyAbi.RegisterAlias("a5"));
    map.put(Pair.of(registerFile, 16), new DummyAbi.RegisterAlias("a6"));
    map.put(Pair.of(registerFile, 17), new DummyAbi.RegisterAlias("a7"));
    map.put(Pair.of(registerFile, 18), new DummyAbi.RegisterAlias("s2"));
    map.put(Pair.of(registerFile, 19), new DummyAbi.RegisterAlias("s3"));
    map.put(Pair.of(registerFile, 20), new DummyAbi.RegisterAlias("s4"));
    map.put(Pair.of(registerFile, 21), new DummyAbi.RegisterAlias("s5"));
    map.put(Pair.of(registerFile, 22), new DummyAbi.RegisterAlias("s6"));
    map.put(Pair.of(registerFile, 23), new DummyAbi.RegisterAlias("s7"));
    map.put(Pair.of(registerFile, 24), new DummyAbi.RegisterAlias("s8"));
    map.put(Pair.of(registerFile, 25), new DummyAbi.RegisterAlias("s9"));
    map.put(Pair.of(registerFile, 26), new DummyAbi.RegisterAlias("s10"));
    map.put(Pair.of(registerFile, 27), new DummyAbi.RegisterAlias("s11"));
    map.put(Pair.of(registerFile, 5), new DummyAbi.RegisterAlias("t0"));
    map.put(Pair.of(registerFile, 6), new DummyAbi.RegisterAlias("t1"));
    map.put(Pair.of(registerFile, 7), new DummyAbi.RegisterAlias("t2"));
    map.put(Pair.of(registerFile, 28), new DummyAbi.RegisterAlias("t3"));
    map.put(Pair.of(registerFile, 29), new DummyAbi.RegisterAlias("t4"));
    map.put(Pair.of(registerFile, 30), new DummyAbi.RegisterAlias("t5"));
    map.put(Pair.of(registerFile, 31), new DummyAbi.RegisterAlias("t6"));
    return map;
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
