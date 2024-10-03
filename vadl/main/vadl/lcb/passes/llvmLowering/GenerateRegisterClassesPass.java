package vadl.lcb.passes.llvmLowering;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.llvmLowering.tablegen.model.register.TableGenRegister;
import vadl.lcb.passes.llvmLowering.tablegen.model.register.TableGenRegisterClass;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.Pair;
import vadl.viam.Instruction;
import vadl.viam.Register;
import vadl.viam.RegisterFile;
import vadl.viam.Specification;
import vadl.viam.passes.dummyAbi.DummyAbi;

/**
 * Generate register classes from {@link RegisterFile}.
 */
public class GenerateRegisterClassesPass extends Pass {

  public static final String TABLE_GEN_REGISTER_CLASS_SUFFIX = "Class";

  public GenerateRegisterClassesPass(LcbConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("GenerateRegisterClassesPass");
  }

  public record Output(List<TableGenRegisterClass> registerClasses) {

  }

  @Nullable
  @Override
  public Output execute(PassResults passResults, Specification viam) throws IOException {
    var abi = (DummyAbi) viam.definitions().filter(x -> x instanceof DummyAbi).findFirst().get();

    var mainRegisterClasses = getMainRegisterClasses(viam.registerFiles(), abi);
    return new Output(mainRegisterClasses);
  }

  /**
   * Create a list of {@link TableGenRegisterClass} for every {@link RegisterFile} in the
   * {@link Specification}.
   */
  private List<TableGenRegisterClass> getMainRegisterClasses(
      Stream<RegisterFile> registerFiles,
      DummyAbi abi) {
    var configuration = (LcbConfiguration) configuration();
    return
        registerFiles.map(registerFile -> {
          var type = ValueType.from(registerFile.resultType()).get();
          return new TableGenRegisterClass(configuration.processorName(),
              registerFile.identifier.simpleName(), 32, //TODO(kper): hardcoded alignment
              List.of(type), getRegisters(registerFile, abi));
        }).toList();
  }

  private List<TableGenRegister> getRegisters(RegisterFile registerFile, DummyAbi abi) {
    var configuration = (LcbConfiguration) configuration();
    var bitWidth = registerFile.addressType().bitWidth();
    var numberOfRegisters = (int) Math.pow(2, bitWidth);

    return IntStream.range(0, numberOfRegisters).mapToObj(number -> {
      var name = registerFile.identifier.simpleName() + number;
      var alias = Optional.ofNullable(abi.aliases().get(Pair.of(registerFile, number)));
      var altNames = alias.map(
          registerAlias -> String.join(", ", wrapInQuotes(registerAlias.value()),
              wrapInQuotes(registerFile.identifier.simpleName() + number))).stream().toList();
      return new TableGenRegister(configuration.processorName(), name,
          alias.orElse(new DummyAbi.RegisterAlias(registerFile.identifier.simpleName() + number))
              .value(),
          altNames, bitWidth - 1, number);
    }).toList();
  }

  private String wrapInQuotes(String value) {
    return "\"" + value + "\"";
  }
}
