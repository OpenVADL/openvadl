package vadl.lcb.template.lib.Target;

import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.lcb.config.LcbConfiguration;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.tablegen.lowering.TableGenImmediateOperandRenderer;
import vadl.lcb.passes.llvmLowering.tablegen.lowering.TableGenInstructionRenderer;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionImmediateOperand;
import vadl.lcb.template.CommonVarNames;
import vadl.pass.PassKey;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.viam.Instruction;
import vadl.viam.Specification;

/**
 * This file contains the mapping for ISelNodes to MI.
 */
public class EmitInstrInfoTableGenFilePass extends AbstractTemplateRenderingPass {

  private final ProcessorName processorName;

  public EmitInstrInfoTableGenFilePass(LcbConfiguration lcbConfiguration,
                                       ProcessorName processorName) throws IOException {
    super(lcbConfiguration.outputPath());
    this.processorName = processorName;
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/InstrInfo.td";
  }

  @Override
  protected String getOutputPath() {
    return "llvm/lib/Target/" + processorName.value() + "/" + processorName.value()
        + "InstrInfo.td";
  }

  @Override
  protected Map<String, Object> createVariables(final Map<PassKey, Object> passResults,
                                                Specification specification) {
    Map<Instruction, LlvmLoweringPass.LlvmLoweringIntermediateResult> instructions =
        (Map<Instruction, LlvmLoweringPass.LlvmLoweringIntermediateResult>) ensureNonNull(
            passResults.get(new PassKey(LlvmLoweringPass.class.getName())),
            "llvmLowering must exist");

    var tableGenRecords = instructions.entrySet().stream()
        .sorted(
            Comparator.comparing(o -> o.getKey().identifier.simpleName()))
        .map(entry -> {
          var instruction = entry.getKey();
          var result = entry.getValue();
          return new TableGenInstruction(
              instruction.identifier.simpleName(),
              "dummyNamespaceValue",
              instruction,
              new TableGenInstruction.Flags(false, false, false, false, false, false, false, false),
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

    var renderedTableGenRecords = tableGenRecords
        .stream()
        .map(TableGenInstructionRenderer::lower)
        .toList();

    return Map.of(CommonVarNames.NAMESPACE, specification.name(),
        "immediates", renderedImmediates,
        "instructions", renderedTableGenRecords);
  }
}
