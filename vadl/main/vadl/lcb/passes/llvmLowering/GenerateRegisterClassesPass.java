package vadl.lcb.passes.llvmLowering;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.llvmLowering.tablegen.model.register.TableGenRegister;
import vadl.lcb.passes.llvmLowering.tablegen.model.register.TableGenRegisterClass;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.Pair;
import vadl.viam.Abi;
import vadl.viam.RegisterFile;
import vadl.viam.RegisterFile.Constraint;
import vadl.viam.Specification;

/**
 * Generate register classes from {@link RegisterFile}.
 */
public class GenerateRegisterClassesPass extends Pass {

  public GenerateRegisterClassesPass(LcbConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("GenerateRegisterClassesPass");
  }

  /**
   * Represents a {@link Constraint} in LLVM.
   */
  public record LlvmConstraint(ValueType type, int value, TableGenRegister register) {

  }

  /**
   * Contains the output of the pass.
   */
  public record Output(List<TableGenRegisterClass> registerClasses,
                       List<TableGenRegister> registers,
                       List<LlvmConstraint> constraints) {
    /* `registers` do not belong to any register class. */
  }

  @Nullable
  @Override
  public Output execute(PassResults passResults, Specification viam) throws IOException {
    var abi = (Abi) viam.definitions().filter(x -> x instanceof Abi).findFirst().get();
    var configuration = (LcbConfiguration) configuration();

    var registers = viam.registers().map(register -> new TableGenRegister(
        configuration.processorName(),
        register.identifier.simpleName(),
        register.identifier.simpleName(),
        Collections.emptyList(),
        0,
        0,
        Optional.empty()
    )).toList();

    var mainRegisterClasses = getMainRegisterClasses(configuration, viam.registerFiles(), abi);
    var constraints = getConstraints(mainRegisterClasses);
    return new Output(mainRegisterClasses, registers, constraints);
  }

  private List<LlvmConstraint> getConstraints(List<TableGenRegisterClass> mainRegisterClasses) {
    var constraints = new ArrayList<LlvmConstraint>();

    for (var rc : mainRegisterClasses) {
      var registerFile = rc.registerFileRef();
      for (var constraint : registerFile.constraints()) {
        var addr = constraint.address().intValue();
        var value = constraint.value().intValue();

        rc.registers().stream().filter(r -> r.index().isPresent() && r.index().get().equals(addr))
            .findFirst()
            .ifPresent(register -> constraints.add(
                new LlvmConstraint(ValueType.from(registerFile.resultType()).get(),
                    value,
                    register)));

      }
    }
    return constraints;
  }

  /**
   * Create a list of {@link TableGenRegisterClass} for every {@link RegisterFile} in the
   * {@link Specification}.
   */
  private List<TableGenRegisterClass> getMainRegisterClasses(
      LcbConfiguration configuration,
      Stream<RegisterFile> registerFiles,
      Abi abi) {
    return
        registerFiles.map(registerFile -> {
          var type = ValueType.from(registerFile.resultType()).get();
          return new TableGenRegisterClass(configuration.processorName(),
              registerFile.identifier.simpleName(), 32, //TODO(kper): hardcoded alignment
              List.of(type), getRegisters(registerFile, abi),
              registerFile);
        }).toList();
  }

  private List<TableGenRegister> getRegisters(RegisterFile registerFile, Abi abi) {
    var configuration = (LcbConfiguration) configuration();
    var bitWidth = registerFile.addressType().bitWidth();
    var numberOfRegisters = (int) Math.pow(2, bitWidth);

    var registers = new ArrayList<TableGenRegister>();

    // We need to add all the registers into the register class.
    // However, first caller-saved, second callee-saved and finally everything else.
    var all = IntStream.range(0, numberOfRegisters).boxed().collect(Collectors.toSet());
    for (var x : abi.callerSaved()) {
      var reg = tableGenRegister(registerFile, abi, configuration, bitWidth, x.addr());
      registers.add(reg);
      all.remove(x.addr());
    }

    for (var x : abi.calleeSaved()) {
      var reg = tableGenRegister(registerFile, abi, configuration, bitWidth, x.addr());
      registers.add(reg);
      all.remove(x.addr());
    }

    for (var addr :
        all) {
      var reg = tableGenRegister(registerFile, abi, configuration, bitWidth, addr);
      registers.add(reg);
    }

    return registers;
  }

  private @Nonnull TableGenRegister tableGenRegister(RegisterFile registerFile, Abi abi,
                                                     LcbConfiguration configuration, int bitWidth,
                                                     int addr) {
    var name = registerFile.identifier.simpleName() + addr;
    var alias = Optional.ofNullable(abi.aliases().get(Pair.of(registerFile, addr)));
    var altNames = alias.map(
        registerAlias -> String.join(", ", wrapInQuotes(registerAlias.value()),
            wrapInQuotes(registerFile.identifier.simpleName() + addr))).stream().toList();
    var reg = new TableGenRegister(configuration.processorName(), name,
        alias.orElse(new Abi.RegisterAlias(registerFile.identifier.simpleName() + addr))
            .value(),
        altNames, bitWidth - 1, addr, Optional.of(addr));
    return reg;
  }

  private String wrapInQuotes(String value) {
    return "\"" + value + "\"";
  }
}
