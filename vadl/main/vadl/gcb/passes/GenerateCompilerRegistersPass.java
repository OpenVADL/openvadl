package vadl.gcb.passes;

import static vadl.viam.ViamError.ensureNonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.error.Diagnostic;
import vadl.gcb.valuetypes.CompilerRegister;
import vadl.gcb.valuetypes.CompilerRegisterClass;
import vadl.gcb.valuetypes.GeneralCompilerRegister;
import vadl.gcb.valuetypes.IndexedCompilerRegister;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Abi;
import vadl.viam.Register;
import vadl.viam.RegisterFile;
import vadl.viam.Specification;

/**
 * The VIAM gives us register files, but we need separate registers.
 */
public class GenerateCompilerRegistersPass extends Pass {
  public GenerateCompilerRegistersPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("GenerateRegistersPass");
  }

  /**
   * Output of the pass.
   */
  public record Output(
      List<CompilerRegister> generalRegisters,
      List<CompilerRegisterClass> registerClasses
  ) {

  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var abi = (Abi) viam.definitions().filter(x -> x instanceof Abi).findFirst().get();

    var generalRegisters = generalRegisters(viam.registers().toList());
    int dwarfOffset = generalRegisters.size();
    var registerClasses = registerClasses(viam.registerFiles().toList(), abi, dwarfOffset);

    return new Output(generalRegisters, registerClasses);
  }

  private List<CompilerRegister> generalRegisters(List<Register> registers) {
    var compilerRegisters = new ArrayList<CompilerRegister>();
    int dwarfOffset = 0;

    for (var register : registers) {
      // The alias should be the same as the register name.
      var alias = GeneralCompilerRegister.generateName(register);

      var compilerRegister =
          new GeneralCompilerRegister(register, alias, Collections.emptyList(), dwarfOffset++);
      compilerRegisters.add(compilerRegister);
    }

    return compilerRegisters;
  }

  private List<CompilerRegisterClass> registerClasses(List<RegisterFile> registerFiles,
                                                      Abi abi,
                                                      int dwarfOffset) {
    var result = new ArrayList<CompilerRegisterClass>();

    for (var registerFile : registerFiles) {
      var registers = IndexedCompilerRegister.fromRegisterFile(registerFile, abi, dwarfOffset);
      dwarfOffset += registers.size();

      var alignment = ensureNonNull(abi.registerFileAlignment().get(registerFile),
          () -> Diagnostic.error("There is not alignment for the register file defined",
              registerFile.sourceLocation().join(abi.sourceLocation())));

      result.add(new CompilerRegisterClass(registerFile, registers, alignment));
    }

    return result;
  }
}
