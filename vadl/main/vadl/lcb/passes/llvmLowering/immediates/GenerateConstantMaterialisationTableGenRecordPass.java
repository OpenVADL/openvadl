package vadl.lcb.passes.llvmLowering.immediates;

import static vadl.lcb.passes.llvmLowering.strategies.LlvmInstructionLoweringStrategy.generateTableGenInputOutput;
import static vadl.viam.ViamError.ensurePresent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.LcbConfiguration;
import vadl.error.Diagnostic;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.domain.ConstantMatPseudoInstruction;
import vadl.lcb.passes.llvmLowering.domain.machineDag.PseudoInstructionNode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmConstantSD;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionImmediateOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPseudoInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenSelectionWithOutputPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.parameterIdentity.ParameterTypeAndNameIdentity;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Constant;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.graph.dependency.WriteRegFileNode;

/**
 * This pass generates a {@link TableGenPseudoInstruction} from a
 * {@link ConstantMatPseudoInstruction}.
 */
public class GenerateConstantMaterialisationTableGenRecordPass extends Pass {

  public GenerateConstantMaterialisationTableGenRecordPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("GenerateConstantMaterialisationTableGenRecordPass");
  }

  private Stream<WriteRegFileNode> writeRegFileNodes(Graph behavior) {
    return behavior.getNodes(InstrCallNode.class)
        .map(InstrCallNode::target)
        .map(Instruction::behavior)
        .flatMap(i -> i.getNodes(WriteRegFileNode.class));
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var records = new ArrayList<TableGenPseudoInstruction>();
    var instructions = (List<ConstantMatPseudoInstruction>) passResults.lastResultOf(
        GenerateConstantMaterialisationPass.class);
    var configuration = (LcbConfiguration) configuration();
    for (var instruction : instructions) {
      var writeRegFileNode =
          ensurePresent(
              writeRegFileNodes(instruction.behavior())
                  .findFirst(), () ->
                  Diagnostic.error("Cannot find register file for constant materialisation",
                      instruction.sourceLocation())
          );

      /*
      def : Pat<(vadl_uimm32_immediateAsInt32:$imm),
        (PSEUDO_CONST_MAT1 vadl_uimm32_immediateAsInt32:$imm)>;
       */
      var selector = new Graph("selector");
      var selectorContent = instruction.immediateRecord().fullname() + ":$imm";
      selector.add(new LlvmConstantSD(
          new Constant.Str("(" + selectorContent + ")")));
      var machine = new Graph("machine");
      machine.addWithInputs(new PseudoInstructionNode(new NodeList<>(
          new LlvmConstantSD(new Constant.Str(selectorContent))),
          instruction));

      List<TableGenPattern> patterns =
          List.of(new TableGenSelectionWithOutputPattern(selector, machine));

      records.add(new TableGenPseudoInstruction(
          instruction.simpleName(),
          configuration.processorName().value(),
          new LlvmLoweringPass.Flags(false, false, false, false, true, true, false, false),
          List.of(new TableGenInstructionImmediateOperand(
              new ParameterTypeAndNameIdentity(instruction.immediateRecord().fullname(), "imm"),
              instruction.immediateRecord())),
          List.of(generateTableGenInputOutput(writeRegFileNode)),
          Collections.emptyList(),
          Collections.emptyList(),
          patterns
      ));
    }

    return records;
  }
}
