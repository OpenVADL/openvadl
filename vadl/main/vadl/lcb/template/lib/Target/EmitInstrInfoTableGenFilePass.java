package vadl.lcb.template.lib.Target;

import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.domain.LlvmLoweringRecord;
import vadl.lcb.passes.llvmLowering.tablegen.lowering.TableGenImmediateOperandRenderer;
import vadl.lcb.passes.llvmLowering.tablegen.lowering.TableGenInstructionRenderer;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionImmediateLabelOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionImmediateOperand;
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
    var llvmLoweringPassResult =
        (LlvmLoweringPass.LlvmLoweringPassResult) ensureNonNull(
            passResults.lastResultOf(LlvmLoweringPass.class),
            "llvmLowering must exist");
    var instructions = llvmLoweringPassResult.machineInstructionRecords();

    var tableGenRecords = instructions.entrySet().stream()
        .sorted(
            Comparator.comparing(o -> o.getKey().identifier.simpleName()))
        .map(entry -> {
          var instruction = entry.getKey();
          var result = entry.getValue();
          return new TableGenInstruction(
              instruction.identifier.simpleName(),
              lcbConfiguration().processorName().value(),
              instruction,
              result.flags(),
              result.inputs(),
              result.outputs(),
              result.uses(),
              result.defs(),
              result.patterns()
          );
        })
        .toList();

    var renderedImmediates = tableGenRecords
        .stream()
        .flatMap(tableGenRecord -> tableGenRecord.getInOperands().stream())
        .filter(operand -> operand instanceof TableGenInstructionImmediateOperand)
        .map(operand -> ((TableGenInstructionImmediateOperand) operand).immediateOperand())
        .distinct()
        .map(TableGenImmediateOperandRenderer::lower)
        .toList();

    var renderedImmediateLabels = tableGenRecords
        .stream()
        .flatMap(tableGenRecord -> tableGenRecord.getInOperands().stream())
        .filter(operand -> operand instanceof TableGenInstructionImmediateLabelOperand)
        .map(operand -> ((TableGenInstructionImmediateLabelOperand) operand).immediateOperand())
        .distinct()
        .map(TableGenImmediateOperandRenderer::lower)
        .toList();

    var renderedTableGenRecords = tableGenRecords
        .stream()
        .map(TableGenInstructionRenderer::lower)
        .toList();

    return Map.of(CommonVarNames.NAMESPACE, specification.name(),
        "stackPointerType",
        ValueType.from(abi.stackPointer().registerFile().resultType()).getLlvmType(),
        "immediates", Stream.concat(renderedImmediates.stream(), renderedImmediateLabels.stream()),
        "instructions", renderedTableGenRecords);
  }
}
