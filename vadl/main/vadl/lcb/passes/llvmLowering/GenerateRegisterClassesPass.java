package vadl.lcb.passes.llvmLowering;

import java.io.IOException;
import java.util.ArrayList;
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
import vadl.viam.RegisterFile;
import vadl.viam.Specification;
import vadl.viam.passes.dummyAbi.DummyAbi;

/**
 * Pseudo instructions contain multiple {@link Instruction} which they should be expanded to.
 * It is possible to redefine the arguments of each instruction. Therefore, a {@link RegisterFile}
 * becomes fixated to a register. TableGen cannot generate instruction patterns for single registers.
 * To fix that, we must create a separate register class for every single register which then
 * TableGen can match.
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

  record Output(List<TableGenRegisterClass> registerClasses) {

  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var abi = (DummyAbi) viam.definitions().filter(x -> x instanceof DummyAbi).findFirst().get();

    var mainRegisterClasses = getMainRegisterClasses(viam.registerFiles(), abi);
    var registerClassesForEachRegisterClass = getIndividualRegisterClasses(mainRegisterClasses);
    return new Output(
        Stream.concat(mainRegisterClasses.stream(), registerClassesForEachRegisterClass.stream())
            .toList());
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
          var type = ValueType.from(registerFile.resultType());
          return new TableGenRegisterClass(configuration.processorName(),
              registerFile.identifier.simpleName(), 32, //TODO(kper): hardcoded alignment
              List.of(type), getRegisters(registerFile, abi));
        }).toList();
  }

  /**
   * TableGen cannot match direct registers. When you specify an {@link Instruction} which adds
   * two registers together and the register's indices are hardcoded then we need a
   * {@link TableGenRegisterClass} with only one register in it to match it correctly.
   * This function generates a {@link TableGenRegisterClass} from every {@link TableGenRegister} in
   * the given {@code mainRegisterClasses}.
   */
  private List<TableGenRegisterClass> getIndividualRegisterClasses(
      List<TableGenRegisterClass> mainRegisterClasses) {
    var result = new ArrayList<TableGenRegisterClass>();

    for (var rg : mainRegisterClasses) {
      for (var register : rg.registers()) {
        var tableGenRegisterClass = new TableGenRegisterClass(
            rg.namespace(),
            register.registerName() + TABLE_GEN_REGISTER_CLASS_SUFFIX,
            rg.alignment(),
            rg.regTypes(),
            List.of(register)
        );
        result.add(tableGenRegisterClass);
      }
    }

    return result;
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
          alias.orElse(new DummyAbi.RegisterAlias(registerFile.identifier.simpleName())).value(),
          altNames, bitWidth - 1, number);
    }).toList();
  }

  private String wrapInQuotes(String value) {
    return "\"" + value + "\"";
  }
}
