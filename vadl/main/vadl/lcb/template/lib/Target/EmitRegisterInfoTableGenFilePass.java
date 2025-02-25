package vadl.lcb.template.lib.Target;

import static vadl.viam.ViamError.ensurePresent;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import vadl.configuration.LcbConfiguration;
import vadl.error.Diagnostic;
import vadl.lcb.passes.llvmLowering.GenerateTableGenRegistersPass;
import vadl.lcb.passes.llvmLowering.tablegen.model.register.TableGenRegisterClass;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.template.Renderable;
import vadl.viam.Abi;
import vadl.viam.Specification;

/**
 * This file contains the register definitions for compiler backend.
 */
public class EmitRegisterInfoTableGenFilePass extends LcbTemplateRenderingPass {

  public EmitRegisterInfoTableGenFilePass(LcbConfiguration lcbConfiguration) throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/RegisterInfo.td";
  }

  @Override
  protected String getOutputPath() {
    var processorName = lcbConfiguration().processorName().value();
    return "llvm/lib/Target/" + processorName + "/" + processorName
        + "RegisterInfo.td";
  }

  record WrappedRegisterFile(TableGenRegisterClass registerFile, String allocationSequence)
      implements
      Renderable {

    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "registerFile", Map.of(
              "name", registerFile.name(),
              "namespace", registerFile.namespace().value(),
              "regTypesString", registerFile.regTypesString(),
              "alignment", registerFile.alignment()
          ),
          "allocationSequence", allocationSequence
      );
    }
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var output = ((GenerateTableGenRegistersPass.Output) passResults.lastResultOf(
        GenerateTableGenRegistersPass.class));
    var abi = (Abi) specification.definitions().filter(x -> x instanceof Abi).findFirst().get();
    var registerClasses = output.registerClasses();

    if (registerClasses.size() > 1) {
      throw Diagnostic.error("Supporting only one register file", specification.sourceLocation())
          .build();
    }

    var registerClass = ensurePresent(registerClasses.stream().findFirst(), "must be present");


    // The order of registers represents the preferred allocation sequence.
    // Registers are listed in the order caller-save, callee-save, specials.
    var callerSaved = abi.callerSaved().stream().map(Abi.RegisterRef::render).toList();

    // Remove marked regs from callee to mark sure that they are allocated last.
    var exceptions = new HashSet<>(List.of(
        abi.returnAddress().render(),
        abi.stackPointer().render(),
        abi.globalPointer().render(),
        abi.framePointer().render(),
        abi.threadPointer().render()
    ));
    var calleeSaved = abi.calleeSaved().stream()
        .map(Abi.RegisterRef::render)
        .filter(render -> !exceptions.contains(render)).toList();

    HashSet<String> both = new HashSet<>();
    both.addAll(callerSaved);
    both.addAll(calleeSaved);
    var specials =
        registerClass.registers().stream().map(
                register -> register.compilerRegister().name()).filter(x -> !both.contains(x))
            .toList();
    var allocationSeq =
        Stream.concat(callerSaved.stream(), Stream.concat(calleeSaved.stream(), specials.stream()))
            .collect(
                Collectors.joining(", "));

    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().processorName().value().toLowerCase(),
        "registers", output.registers(),
        "registerFiles", List.of(
            new WrappedRegisterFile(registerClass, allocationSeq)
        ));
  }
}
