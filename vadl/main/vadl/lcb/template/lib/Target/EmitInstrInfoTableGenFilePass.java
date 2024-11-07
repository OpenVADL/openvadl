package vadl.lcb.template.lib.Target;

import static vadl.viam.ViamError.ensurePresent;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.error.Diagnostic;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.isaMatching.IsaMachineInstructionMatchingPass;
import vadl.lcb.passes.isaMatching.MachineInstructionLabel;
import vadl.lcb.passes.llvmLowering.GenerateTableGenMachineInstructionRecordPass;
import vadl.lcb.passes.llvmLowering.GenerateTableGenPseudoInstructionRecordPass;
import vadl.lcb.passes.llvmLowering.immediates.GenerateConstantMaterialisationTableGenRecordPass;
import vadl.lcb.passes.llvmLowering.immediates.GenerateTableGenImmediateRecordPass;
import vadl.lcb.passes.llvmLowering.tablegen.lowering.TableGenImmediateOperandRenderer;
import vadl.lcb.passes.llvmLowering.tablegen.lowering.TableGenInstructionRenderer;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenImmediateRecord;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenMachineInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPseudoInstruction;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.passes.dummyAbi.DummyAbi;

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
        (DummyAbi) specification.definitions().filter(x -> x instanceof DummyAbi).findFirst().get();
    var tableGenMachineRecords = (List<TableGenMachineInstruction>) passResults.lastResultOf(
        GenerateTableGenMachineInstructionRecordPass.class);
    var tableGenPseudoRecords = (List<TableGenPseudoInstruction>) passResults.lastResultOf(
        GenerateTableGenPseudoInstructionRecordPass.class);
    var tableGenConstMatRecords = ((List<TableGenPseudoInstruction>) passResults.lastResultOf(
        GenerateConstantMaterialisationTableGenRecordPass.class));
    var labelledMachineInstructions = (HashMap<MachineInstructionLabel, List<Instruction>>)
        passResults.lastResultOf(IsaMachineInstructionMatchingPass.class);

    var addi =
        ensurePresent(labelledMachineInstructions.getOrDefault(MachineInstructionLabel.ADDI_64,
                    labelledMachineInstructions.get(MachineInstructionLabel.ADDI_32))
                .stream().findFirst(),
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

    var renderedTableGenConstMatRecords = tableGenConstMatRecords
        .stream()
        .map(TableGenInstructionRenderer::lower)
        .toList();

    return Map.of(CommonVarNames.NAMESPACE, specification.simpleName(),
        "addi", addi,
        "stackPointerRegister", abi.stackPointer(),
        "stackPointerType",
        ValueType.from(abi.stackPointer().registerFile().resultType()).get().getLlvmType(),
        "immediates", renderedImmediates,
        "instructions", renderedTableGenMachineRecords,
        "pseudos", renderedTableGenPseudoRecords,
        "constMats", renderedTableGenConstMatRecords);
  }
}
