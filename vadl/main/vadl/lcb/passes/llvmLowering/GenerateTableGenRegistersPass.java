package vadl.lcb.passes.llvmLowering;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import vadl.configuration.LcbConfiguration;
import vadl.gcb.passes.GenerateCompilerRegistersPass;
import vadl.gcb.valuetypes.CompilerRegister;
import vadl.gcb.valuetypes.CompilerRegisterClass;
import vadl.gcb.valuetypes.IndexedCompilerRegister;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.llvmLowering.tablegen.model.register.TableGenRegister;
import vadl.lcb.passes.llvmLowering.tablegen.model.register.TableGenRegisterClass;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.RegisterFile.Constraint;
import vadl.viam.Specification;

/**
 * Generate registers, register classes from {@link CompilerRegister} and
 * {@link CompilerRegisterClass} which were generated in {@link GenerateCompilerRegistersPass}.
 */
public class GenerateTableGenRegistersPass extends Pass {

  public GenerateTableGenRegistersPass(LcbConfiguration configuration) {
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
    var configuration = (LcbConfiguration) configuration();
    var output = (GenerateCompilerRegistersPass.Output) passResults.lastResultOf(
        GenerateCompilerRegistersPass.class);
    var compilerRegisterClasses = output.registerClasses();

    var registerClasses = new ArrayList<TableGenRegisterClass>();
    var registers = new ArrayList<TableGenRegister>();

    for (var compilerRegister : output.generalRegisters()) {
      var register = new TableGenRegister(
          configuration.processorName(),
          compilerRegister,
          compilerRegister.hwEncodingValue(),
          Optional.empty()
      );
      registers.add(register);
    }

    for (var compilerRegisterClass : compilerRegisterClasses) {
      var classRegisters = new ArrayList<TableGenRegister>();
      for (var compilerRegister : compilerRegisterClass.registers()) {
        var register = new TableGenRegister(
            configuration.processorName(),
            compilerRegister,
            /*List.of(compilerRegister.asmName(), compilerRegister.name()),*/
            compilerRegisterClass.registerFile().addressType().bitWidth() - 1,
            Optional.of(compilerRegister.hwEncodingValue())
        );
        registers.add(register);
        classRegisters.add(register);
      }

      var type = ValueType.from(compilerRegisterClass.registerFile().resultType()).get();
      registerClasses.add(
          new TableGenRegisterClass(
              configuration.processorName(),
              compilerRegisterClass.name(),
              compilerRegisterClass.alignment().bitAlignment(),
              List.of(type),
              classRegisters,
              compilerRegisterClass.registerFile())
      );
    }

    var constraints = getConstraints(registerClasses);
    return new Output(registerClasses, registers, constraints);
  }

  private List<LlvmConstraint> getConstraints(List<TableGenRegisterClass> mainRegisterClasses) {
    var constraints = new ArrayList<LlvmConstraint>();

    for (var rc : mainRegisterClasses) {
      var registerFile = rc.registerFileRef();
      for (var constraint : registerFile.constraints()) {
        var addr = constraint.address().intValue();
        var value = constraint.value().intValue();

        rc.registers().stream().filter(
                r -> r.compilerRegister() instanceof IndexedCompilerRegister reg
                    && reg.index() == addr)
            .findFirst()
            .ifPresent(register -> constraints.add(
                new LlvmConstraint(ValueType.from(registerFile.resultType()).get(),
                    value,
                    register)));

      }
    }
    return constraints;
  }
}
