package vadl.lcb.template.lib.Target.MCTargetDesc;

import static vadl.viam.ViamError.ensureNonNull;
import static vadl.viam.ViamError.ensurePresent;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import vadl.configuration.LcbConfiguration;
import vadl.error.Diagnostic;
import vadl.gcb.passes.IdentifyFieldUsagePass;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.isaMatching.IsaMachineInstructionMatchingPass;
import vadl.lcb.passes.isaMatching.MachineInstructionLabel;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Format;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.passes.dummyAbi.DummyAbi;

/**
 * This file contains the implementation for constant materialisation.
 */
public class EmitConstMatIntHeaderFilePass extends LcbTemplateRenderingPass {

  public EmitConstMatIntHeaderFilePass(LcbConfiguration lcbConfiguration)
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
        + "ConstMatInt.h";
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
    var immediateAddiSize = immediateSize(immediateDetection, addi);
    var largestPossibleValueAddi = (long) (Math.pow(2, immediateAddiSize) - 1);
    var lui =
        ensurePresent(
            Objects.requireNonNull(labelledInstructions)
                .getOrDefault(MachineInstructionLabel.LUI, Collections.emptyList())
                .stream().findFirst(),
            () -> Diagnostic.error("Expected an instruction of load upper immediate",
                specification.sourceLocation()));
    var luiImmediate = immediate(immediateDetection, lui).get();
    var immediateLuiSize = immediateSize(immediateDetection, lui);
    var largestPossibleValueLui = (long) (Math.pow(2, immediateLuiSize) - 1);
    int luiFormatSize = lui.format().type().bitWidth();
    return Map.of(CommonVarNames.NAMESPACE, specification.simpleName(),
        "addi", addi.identifier.simpleName(),
        "lui", lui.identifier.simpleName(),
        "luiHighBit", luiImmediate.bitSlice().msb(),
        "luiLowBit", luiImmediate.bitSlice().lsb(),
        "luiFormatSize", luiFormatSize,
        "addiBitSize", immediateAddiSize - 1,
        "largestPossibleValueAddi", largestPossibleValueAddi,
        "largestPossibleValue", (long) Math.pow(2, lui.format().type().bitWidth()) - 1,
        "largestPossibleValueLui", largestPossibleValueLui
    );
  }

  private static int immediateSize(
      IdentifyFieldUsagePass.ImmediateDetectionContainer immediateDetection,
      Instruction instruction) {
    var immediate = immediate(immediateDetection, instruction);
    return ensurePresent(immediate,
        () -> Diagnostic.error("Compiler generator was not able to get maximal storable value",
            instruction.sourceLocation()))
        .size();
  }

  private static @NotNull Optional<Format.Field> immediate(
      IdentifyFieldUsagePass.ImmediateDetectionContainer immediateDetection,
      Instruction instruction) {
    return immediateDetection.getImmediateUsages(instruction)
        .entrySet()
        .stream()
        .filter(x -> x.getValue() == IdentifyFieldUsagePass.FieldUsage.IMMEDIATE)
        .map(Map.Entry::getKey)
        .findFirst();
  }
}
