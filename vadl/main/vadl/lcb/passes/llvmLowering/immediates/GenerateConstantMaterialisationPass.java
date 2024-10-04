package vadl.lcb.passes.llvmLowering.immediates;

import static vadl.viam.ViamError.ensure;
import static vadl.viam.ViamError.ensurePresent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;
import org.checkerframework.checker.units.qual.C;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.error.Diagnostic;
import vadl.lcb.passes.isaMatching.InstructionLabel;
import vadl.lcb.passes.isaMatching.IsaMatchingPass;
import vadl.lcb.passes.llvmLowering.domain.ConstantMatPseudoInstruction;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmConstantSD;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenImmediateRecord;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.DataType;
import vadl.viam.Constant;
import vadl.viam.Identifier;
import vadl.viam.Instruction;
import vadl.viam.Parameter;
import vadl.viam.PseudoInstruction;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.graph.control.InstrEndNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.WriteResourceNode;

/**
 * This pass generates a {@link PseudoInstruction} which consume immediates and emit a machine
 * instruction which loads the value into the register. Note that we will not be looking for
 * instruction like {@code LI} because it is hard to capture the semantics. Instead, we use
 * {@link IsaMatchingPass} and use the {@code ADDI} as move-instruction.
 */
public class GenerateConstantMaterialisationPass extends Pass {

  public GenerateConstantMaterialisationPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("GenerateConstantMaterialisationPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var constantMatInstructions = new ArrayList<ConstantMatPseudoInstruction>();
    var isaMatching = ((HashMap<InstructionLabel, List<Instruction>>) passResults.lastResultOf(
        IsaMatchingPass.class));
    var immediates = ((List<TableGenImmediateRecord>) passResults.lastResultOf(
        GenerateTableGenImmediateRecordPass.class));

    var additionWithImmediates = isaMatching.getOrDefault(InstructionLabel.ADDI_64,
        isaMatching.getOrDefault(InstructionLabel.ADDI_32, Collections.emptyList()));
    ensure(!additionWithImmediates.isEmpty(), () -> Diagnostic.error(
        "Specification has no instruction for addition with immediate. Therefore, vadl "
            + "cannot generate an instruction for constant materialisation.",
        viam.sourceLocation()).build());
    var addi = additionWithImmediates.stream().findFirst().get();

    for (var imm : immediates) {
      var name = addi.identifier.append(imm.rawName() + "_const_mat");
      var copy = addi.behavior().copy();
      var graph = setupGraph(name, copy, addi);
      var instruction =
          new ConstantMatPseudoInstruction(name,
              new Parameter[] {}, graph, addi.assembly(), imm);
      constantMatInstructions.add(instruction);
    }

    return constantMatInstructions;
  }

  private Graph setupGraph(Identifier name, Graph copy, Instruction addi) {
    var pseudoInstructionGraph = new Graph(name.toString());
    var fields = copy.getNodes(FieldRefNode.class).toList();
    var immField = ensurePresent(copy.getNodes(FieldRefNode.class)
            .filter(fieldNode -> fieldNode.usages().noneMatch(
                usage -> usage instanceof ReadResourceNode || usage instanceof WriteResourceNode))
            .findFirst(),
        () -> Diagnostic.error("Expected immediate in instruction", addi.sourceLocation())
            .build());

    var registerArguments = fields
        .stream()
        .filter(field -> field != immField) // only registers are relevant
        .map(ri -> new FuncParamNode(new Parameter(ri.formatField().identifier, ri.type())))
        .toList();
    var immArgument = new ConstantNode(Constant.Value.of(0, (DataType) immField.type()));
    var arguments = Stream.concat(registerArguments.stream(), Stream.of(immArgument)).toList();
    var instrCallNode =
        new InstrCallNode(addi, fields.stream().map(FieldRefNode::formatField).toList(),
            new NodeList<>(arguments));
    var start = new StartNode(instrCallNode);
    pseudoInstructionGraph.addWithInputs(start);
    pseudoInstructionGraph.addWithInputs(instrCallNode);

    return pseudoInstructionGraph;
  }
}
