package vadl.lcb.template.lib.Target.MCTargetDesc;

import static vadl.viam.ViamError.ensureNonNull;
import static vadl.viam.ViamError.ensurePresent;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import jdk.jshell.Diag;
import vadl.configuration.LcbConfiguration;
import vadl.error.Diagnostic;
import vadl.gcb.passes.IdentifyFieldUsagePass;
import vadl.lcb.passes.isaMatching.IsaMachineInstructionMatchingPass;
import vadl.lcb.passes.isaMatching.MachineInstructionLabel;
import vadl.lcb.passes.relocation.GenerateLinkerComponentsPass;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.passes.dummyAbi.DummyAbi;

/**
 * This file contains the implementation for constant materialisation.
 */
public class EmitMatIntHeaderFilePass extends LcbTemplateRenderingPass {

  public EmitMatIntHeaderFilePass(LcbConfiguration lcbConfiguration)
      throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/MCTargetDesc/TargetConstMatInt.h";
  }

  @Override
  protected String getOutputPath() {
    var processorName = lcbConfiguration().processorName().value();
    return "llvm/lib/Target/" + processorName + "/MCTargetDesc/" + processorName
        + "ConstMat.h";
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var labelledInstructions =
        ensureNonNull(
            (Map<MachineInstructionLabel, List<Instruction>>) passResults.lastResultOf(
                IsaMachineInstructionMatchingPass.class), "labelling must be present");
    var addi =
        ensurePresent(
            Objects.requireNonNull(labelledInstructions)
                .getOrDefault(MachineInstructionLabel.ADDI_64,
                    labelledInstructions.getOrDefault(MachineInstructionLabel.ADDI_32,
                        Collections.emptyList()))
                .stream().findFirst(),
            () -> Diagnostic.error("Expected an instruction with addition of immediate",
                specification.sourceLocation()));
    var immediateDetection =
        (IdentifyFieldUsagePass.ImmediateDetectionContainer) passResults
            .lastResultOf(IdentifyFieldUsagePass.class);
    // Get the immediate in the `addi` instruction.
    var immediate = immediateDetection.getImmediateUsages(addi)
        .entrySet()
        .stream()
        .filter(x -> x.getValue() == IdentifyFieldUsagePass.FieldUsage.IMMEDIATE)
        .map(Map.Entry::getKey)
        .findFirst();
    var immediateSize =
        ensurePresent(immediate,
            () -> Diagnostic.error("Compiler generator was not able to get maximal storable value",
                addi.sourceLocation()))
            .size();
    var largestPossibleValue = Math.pow(2, immediateSize) - 1;

    return Map.of(CommonVarNames.NAMESPACE, specification.simpleName(),
        "addi", addi.identifier.simpleName(),
        "largestPossibleValue", largestPossibleValue);
  }
}
