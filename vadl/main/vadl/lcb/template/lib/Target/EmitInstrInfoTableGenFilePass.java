package vadl.lcb.template.lib.Target;

import static vadl.viam.ViamError.ensurePresent;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import vadl.configuration.LcbConfiguration;
import vadl.error.Diagnostic;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.isaMatching.IsaMachineInstructionMatchingPass;
import vadl.lcb.passes.isaMatching.MachineInstructionLabel;
import vadl.lcb.passes.llvmLowering.GenerateTableGenMachineInstructionRecordPass;
import vadl.lcb.passes.llvmLowering.GenerateTableGenPseudoInstructionRecordPass;
import vadl.lcb.passes.llvmLowering.compensation.CompensationPatternPass;
import vadl.lcb.passes.llvmLowering.immediates.GenerateTableGenImmediateRecordPass;
import vadl.lcb.passes.llvmLowering.tablegen.lowering.TableGenImmediateOperandRenderer;
import vadl.lcb.passes.llvmLowering.tablegen.lowering.TableGenInstructionPatternRenderer;
import vadl.lcb.passes.llvmLowering.tablegen.lowering.TableGenInstructionRenderer;
import vadl.lcb.passes.llvmLowering.tablegen.lowering.TableGenPseudoInstExpansionRenderer;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenImmediateRecord;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenMachineInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPseudoInstExpansionPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPseudoInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenSelectionWithOutputPattern;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Abi;
import vadl.viam.Instruction;
import vadl.viam.Specification;

/**
 * This file contains the mapping for ISelNodes to MI.
 */
public class EmitInstrInfoTableGenFilePass extends LcbTemplateRenderingPass {

  public EmitInstrInfoTableGenFilePass(LcbConfiguration lcbConfiguration) throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/InstrInfo.td";
  }

  @Override
  protected String getOutputPath() {
    var processorName = lcbConfiguration().processorName().value();
    return "llvm/lib/Target/" + processorName + "/" + processorName
        + "InstrInfo.td";
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var abi =
        (Abi) specification.definitions().filter(x -> x instanceof Abi).findFirst().get();
    var tableGenMachineRecords = (List<TableGenMachineInstruction>) passResults.lastResultOf(
        GenerateTableGenMachineInstructionRecordPass.class);
    var tableGenPseudoRecords = (List<TableGenPseudoInstruction>) passResults.lastResultOf(
        GenerateTableGenPseudoInstructionRecordPass.class);
    var labelledMachineInstructions = (Map<MachineInstructionLabel, List<Instruction>>)
        passResults.lastResultOf(IsaMachineInstructionMatchingPass.class);

    var addi32 = labelledMachineInstructions.get(MachineInstructionLabel.ADDI_32);
    var addi64 = labelledMachineInstructions.get(MachineInstructionLabel.ADDI_64);
    var rawAddi = addi64 != null ? addi64 : Objects.requireNonNull(addi32);

    var addi = ensurePresent(rawAddi.stream().findFirst(),
        () -> Diagnostic.error("Instruction set requires an addition with immediate",
            specification.sourceLocation()));

    var renderedImmediates = ((List<TableGenImmediateRecord>) passResults.lastResultOf(
        GenerateTableGenImmediateRecordPass.class))
        .stream()
        .map(TableGenImmediateOperandRenderer::lower)
        .toList();

    var renderedTableGenMachineRecords = tableGenMachineRecords
        .stream()
        .map(TableGenInstructionRenderer::lower)
        .toList();

    var renderedTableGenPseudoRecords = tableGenPseudoRecords
        .stream()
        .map(TableGenInstructionRenderer::lower)
        .toList();

    var pseudoExpansionPatterns = tableGenMachineRecords
        .stream()
        .flatMap(x -> x.getAnonymousPatterns().stream())
        .filter(x -> x instanceof TableGenPseudoInstExpansionPattern)
        .map(x -> (TableGenPseudoInstExpansionPattern) x)
        .toList();

    var compensationPatterns =
        (List<TableGenSelectionWithOutputPattern>) passResults.lastResultOf(
            CompensationPatternPass.class);

    var renderedPatterns =
        Stream.concat(
                Stream.concat(
                    tableGenMachineRecords.stream().map(TableGenInstructionPatternRenderer::lower),
                    Stream.concat(
                        tableGenPseudoRecords.stream()
                            .map(TableGenInstructionPatternRenderer::lower),
                        compensationPatterns.stream()
                            .map(TableGenInstructionPatternRenderer::lower))
                ),
                pseudoExpansionPatterns.stream().map(TableGenPseudoInstExpansionRenderer::lower))
            .toList();

    return Map.of(CommonVarNames.NAMESPACE, specification.simpleName(),
        "addi", addi,
        "stackPointerRegister", abi.stackPointer(),
        "stackPointerType",
        ValueType.from(abi.stackPointer().registerFile().resultType()).get().getLlvmType(),
        "immediates", renderedImmediates,
        "instructions", renderedTableGenMachineRecords,
        "pseudos", renderedTableGenPseudoRecords,
        "patterns", renderedPatterns,
        "registerFiles", specification.registerFiles().toList());
  }
}
