package vadl.lcb.template.lib.Target;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.codegen.model.llvm.ValueType;
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
        "stackPointerRegister", abi.stackPointer(),
        "stackPointerType",
        ValueType.from(abi.stackPointer().registerFile().resultType()).get().getLlvmType(),
        "immediates", renderedImmediates,
        "instructions", renderedTableGenMachineRecords,
        "pseudos", renderedTableGenPseudoRecords,
        "constMats", renderedTableGenConstMatRecords);
  }
}
