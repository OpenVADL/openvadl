package vadl.lcb.template.lib.Target.MCTargetDesc;

import static vadl.viam.ViamError.ensure;
import static vadl.viam.ViamError.ensureNonNull;
import static vadl.viam.ViamError.ensurePresent;
import static vadl.viam.ViamError.unwrap;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import jdk.jshell.Diag;
import org.jetbrains.annotations.NotNull;
import vadl.configuration.LcbConfiguration;
import vadl.error.Diagnostic;
import vadl.gcb.passes.IdentifyFieldUsagePass;
import vadl.lcb.passes.EncodeAssemblyImmediateAnnotation;
import vadl.lcb.passes.isaMatching.IsaMachineInstructionMatchingPass;
import vadl.lcb.passes.isaMatching.MachineInstructionLabel;
import vadl.lcb.passes.llvmLowering.GenerateTableGenMachineInstructionRecordPass;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionImmediateOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenMachineInstruction;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Format;
import vadl.viam.Instruction;
import vadl.viam.Specification;

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
    var slli =
        ensurePresent(
            Objects.requireNonNull(labelledInstructions)
                .getOrDefault(MachineInstructionLabel.SLLI,
                    Collections.emptyList())
                .stream().findFirst(),
            () -> Diagnostic.error("Expected an instruction with addition of immediate",
                specification.sourceLocation()));
    var immediateDetection =
        (IdentifyFieldUsagePass.ImmediateDetectionContainer) passResults
            .lastResultOf(IdentifyFieldUsagePass.class);
    var immediateAddiSize = immediateSize(immediateDetection, addi);
    var largestPossibleValueAddi = (long) (Math.pow(2, immediateAddiSize) / 2) - 1;
    var smallestPossibleValueAddi = (long) (Math.pow(2, immediateAddiSize) / -2);
    var lui =
        ensurePresent(
            Objects.requireNonNull(labelledInstructions)
                .getOrDefault(MachineInstructionLabel.LUI, Collections.emptyList())
                .stream().findFirst(),
            () -> Diagnostic.error("Expected an instruction of load upper immediate",
                specification.sourceLocation()));
    ensure(lui.assembly().hasAnnotation(EncodeAssemblyImmediateAnnotation.class),
        () -> Diagnostic.error(
                "Load upper immediate machine instruction has no encoding immediate function.",
                lui.sourceLocation())
            .note(
                "The compiler expects that the load upper immediate has an annotation for " +
                    "formatting the immediate. This is important for the constant materialisation."));
    var luiImmediate = immediate(immediateDetection, lui).get();
    var immediateLuiSize = immediateSize(immediateDetection, lui);
    var largestPossibleValueLui = (long) (Math.pow(2, immediateLuiSize) - 1);
    int luiFormatSize = lui.format().type().bitWidth();

    var map = new HashMap<String, Object>();
    map.put(CommonVarNames.NAMESPACE, specification.simpleName());
    map.put("addi", addi.identifier.simpleName());
    map.put("lui", lui.identifier.simpleName());
    map.put("luiRawEncoderMethod", rawEncoderMethodForLui(passResults, lui));
    map.put("slli", slli.identifier.simpleName());
    map.put("luiHighBit", luiImmediate.bitSlice().msb());
    map.put("luiLowBit", luiImmediate.bitSlice().lsb());
    map.put("luiFormatSize", luiFormatSize);
    map.put("addiBitSize", immediateAddiSize - 1);
    map.put("largestPossibleValueAddi", largestPossibleValueAddi);
    map.put("smallestPossibleValueAddi", smallestPossibleValueAddi);
    map.put("largestPossibleValue", (long) Math.pow(2, lui.format().type().bitWidth()) - 1);
    map.put("largestPossibleValueLui", largestPossibleValueLui);

    return map;
  }

  /**
   * To format the value of LUI, we need to call the encoder method. This method gets the name
   * of the method.
   */
  private String rawEncoderMethodForLui(PassResults passResults, Instruction lui) {
    var machineRecords = (List<TableGenMachineInstruction>) passResults.lastResultOf(
        GenerateTableGenMachineInstructionRecordPass.class);

    var record = unwrap(machineRecords.stream()
        .filter(x -> x.instruction() == lui)
        .findFirst());

    return
        ensurePresent(
            record.getInOperands()
                .stream()
                .filter(x -> x instanceof TableGenInstructionImmediateOperand)
                .map(y -> (TableGenInstructionImmediateOperand) y)
                .map(x -> x.immediateOperand().rawEncoderMethod())
                .findFirst(),
            () -> Diagnostic.error(
                "Cannot find encoder method for load upper immediate instruction",
                lui.sourceLocation())
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
