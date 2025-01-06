package vadl.viam.passes.dummyPasses;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.Pair;
import vadl.utils.SourceLocation;
import vadl.viam.Abi;
import vadl.viam.Identifier;
import vadl.viam.PseudoInstruction;
import vadl.viam.RegisterFile;
import vadl.viam.Specification;

/**
 * Inserts a hardcoded {@link Abi} to the {@link Specification} for RISC-V.
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

    viam.add(new Abi(new Identifier("dummyAbi", SourceLocation.INVALID_SOURCE_LOCATION),
        new Abi.RegisterRef(registerFile, 1, Abi.Alignment.WORD),
        new Abi.RegisterRef(registerFile, 2, Abi.Alignment.HALF_WORD),
        new Abi.RegisterRef(registerFile, 8, Abi.Alignment.WORD),
        new Abi.RegisterRef(registerFile, 3, Abi.Alignment.WORD),
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

  private List<Abi.RegisterRef> getReturnRegisters(RegisterFile registerFile) {
    return List.of(
        new Abi.RegisterRef(registerFile, 10, Abi.Alignment.NO_ALIGNMENT),
        new Abi.RegisterRef(registerFile, 11, Abi.Alignment.NO_ALIGNMENT)
    );
  }

  private List<Abi.RegisterRef> getArgumentRegisters(RegisterFile registerFile) {
    return List.of(
        new Abi.RegisterRef(registerFile, 10, Abi.Alignment.NO_ALIGNMENT),
        new Abi.RegisterRef(registerFile, 11, Abi.Alignment.NO_ALIGNMENT),
        new Abi.RegisterRef(registerFile, 12, Abi.Alignment.NO_ALIGNMENT),
        new Abi.RegisterRef(registerFile, 13, Abi.Alignment.NO_ALIGNMENT),
        new Abi.RegisterRef(registerFile, 14, Abi.Alignment.NO_ALIGNMENT),
        new Abi.RegisterRef(registerFile, 15, Abi.Alignment.NO_ALIGNMENT),
        new Abi.RegisterRef(registerFile, 16, Abi.Alignment.NO_ALIGNMENT),
        new Abi.RegisterRef(registerFile, 17, Abi.Alignment.NO_ALIGNMENT)
    );
  }

  private Map<Pair<RegisterFile, Integer>, Abi.RegisterAlias> getAliases(
      RegisterFile registerFile) {
    var map = new HashMap<Pair<RegisterFile, Integer>, Abi.RegisterAlias>();
    map.put(Pair.of(registerFile, 0), new Abi.RegisterAlias("zero"));
    map.put(Pair.of(registerFile, 1), new Abi.RegisterAlias("ra"));
    map.put(Pair.of(registerFile, 2), new Abi.RegisterAlias("sp"));
    map.put(Pair.of(registerFile, 3), new Abi.RegisterAlias("gp"));
    map.put(Pair.of(registerFile, 4), new Abi.RegisterAlias("tp"));
    map.put(Pair.of(registerFile, 8), new Abi.RegisterAlias("fp"));
    map.put(Pair.of(registerFile, 9), new Abi.RegisterAlias("s1"));
    map.put(Pair.of(registerFile, 10), new Abi.RegisterAlias("a0"));
    map.put(Pair.of(registerFile, 11), new Abi.RegisterAlias("a1"));
    map.put(Pair.of(registerFile, 12), new Abi.RegisterAlias("a2"));
    map.put(Pair.of(registerFile, 13), new Abi.RegisterAlias("a3"));
    map.put(Pair.of(registerFile, 14), new Abi.RegisterAlias("a4"));
    map.put(Pair.of(registerFile, 15), new Abi.RegisterAlias("a5"));
    map.put(Pair.of(registerFile, 16), new Abi.RegisterAlias("a6"));
    map.put(Pair.of(registerFile, 17), new Abi.RegisterAlias("a7"));
    map.put(Pair.of(registerFile, 18), new Abi.RegisterAlias("s2"));
    map.put(Pair.of(registerFile, 19), new Abi.RegisterAlias("s3"));
    map.put(Pair.of(registerFile, 20), new Abi.RegisterAlias("s4"));
    map.put(Pair.of(registerFile, 21), new Abi.RegisterAlias("s5"));
    map.put(Pair.of(registerFile, 22), new Abi.RegisterAlias("s6"));
    map.put(Pair.of(registerFile, 23), new Abi.RegisterAlias("s7"));
    map.put(Pair.of(registerFile, 24), new Abi.RegisterAlias("s8"));
    map.put(Pair.of(registerFile, 25), new Abi.RegisterAlias("s9"));
    map.put(Pair.of(registerFile, 26), new Abi.RegisterAlias("s10"));
    map.put(Pair.of(registerFile, 27), new Abi.RegisterAlias("s11"));
    map.put(Pair.of(registerFile, 5), new Abi.RegisterAlias("t0"));
    map.put(Pair.of(registerFile, 6), new Abi.RegisterAlias("t1"));
    map.put(Pair.of(registerFile, 7), new Abi.RegisterAlias("t2"));
    map.put(Pair.of(registerFile, 28), new Abi.RegisterAlias("t3"));
    map.put(Pair.of(registerFile, 29), new Abi.RegisterAlias("t4"));
    map.put(Pair.of(registerFile, 30), new Abi.RegisterAlias("t5"));
    map.put(Pair.of(registerFile, 31), new Abi.RegisterAlias("t6"));
    return map;
  }

  private List<Abi.RegisterRef> getCalleeSaved(RegisterFile registerFile) {
    return List.of(
        //new Abi.RegisterRef(registerFile, 2, Abi.Alignment.NO_ALIGNMENT),
        new Abi.RegisterRef(registerFile, 8, Abi.Alignment.NO_ALIGNMENT),
        new Abi.RegisterRef(registerFile, 9, Abi.Alignment.NO_ALIGNMENT),
        new Abi.RegisterRef(registerFile, 18, Abi.Alignment.NO_ALIGNMENT),
        new Abi.RegisterRef(registerFile, 19, Abi.Alignment.NO_ALIGNMENT),
        new Abi.RegisterRef(registerFile, 20, Abi.Alignment.NO_ALIGNMENT),
        new Abi.RegisterRef(registerFile, 21, Abi.Alignment.NO_ALIGNMENT),
        new Abi.RegisterRef(registerFile, 22, Abi.Alignment.NO_ALIGNMENT),
        new Abi.RegisterRef(registerFile, 23, Abi.Alignment.NO_ALIGNMENT),
        new Abi.RegisterRef(registerFile, 24, Abi.Alignment.NO_ALIGNMENT),
        new Abi.RegisterRef(registerFile, 25, Abi.Alignment.NO_ALIGNMENT),
        new Abi.RegisterRef(registerFile, 26, Abi.Alignment.NO_ALIGNMENT),
        new Abi.RegisterRef(registerFile, 27, Abi.Alignment.NO_ALIGNMENT),
        new Abi.RegisterRef(registerFile, 1, Abi.Alignment.NO_ALIGNMENT)
        );
  }

  private List<Abi.RegisterRef> getCallerSaved(RegisterFile registerFile) {
    return List.of(
        new Abi.RegisterRef(registerFile, 10, Abi.Alignment.NO_ALIGNMENT),
        new Abi.RegisterRef(registerFile, 11, Abi.Alignment.NO_ALIGNMENT),
        new Abi.RegisterRef(registerFile, 12, Abi.Alignment.NO_ALIGNMENT),
        new Abi.RegisterRef(registerFile, 13, Abi.Alignment.NO_ALIGNMENT),
        new Abi.RegisterRef(registerFile, 14, Abi.Alignment.NO_ALIGNMENT),
        new Abi.RegisterRef(registerFile, 15, Abi.Alignment.NO_ALIGNMENT),
        new Abi.RegisterRef(registerFile, 16, Abi.Alignment.NO_ALIGNMENT),
        new Abi.RegisterRef(registerFile, 17, Abi.Alignment.NO_ALIGNMENT),
        new Abi.RegisterRef(registerFile, 5, Abi.Alignment.NO_ALIGNMENT),
        new Abi.RegisterRef(registerFile, 6, Abi.Alignment.NO_ALIGNMENT),
        new Abi.RegisterRef(registerFile, 7, Abi.Alignment.NO_ALIGNMENT),
        new Abi.RegisterRef(registerFile, 28, Abi.Alignment.NO_ALIGNMENT),
        new Abi.RegisterRef(registerFile, 29, Abi.Alignment.NO_ALIGNMENT),
        new Abi.RegisterRef(registerFile, 30, Abi.Alignment.NO_ALIGNMENT),
        new Abi.RegisterRef(registerFile, 31, Abi.Alignment.NO_ALIGNMENT)
    );
  }
}
