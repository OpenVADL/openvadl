package vadl.lcb.passes.llvmLowering.immediates;

import static vadl.viam.ViamError.ensure;
import static vadl.viam.ViamError.ensureNonNull;
import static vadl.viam.ViamError.ensurePresent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.error.Diagnostic;
import vadl.gcb.passes.IdentifyFieldUsagePass;
import vadl.lcb.passes.isaMatching.InstructionLabel;
import vadl.lcb.passes.isaMatching.IsaMatchingPass;
import vadl.lcb.passes.llvmLowering.domain.ConstantMatPseudoInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenImmediateRecord;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Constant;
import vadl.viam.Identifier;
import vadl.viam.Instruction;
import vadl.viam.Parameter;
import vadl.viam.PseudoInstruction;
import vadl.viam.RegisterFile;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.HasRegisterFile;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.WriteRegFileNode;
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
    var fieldUsages = (IdentifyFieldUsagePass.ImmediateDetectionContainer) passResults.lastResultOf(
        IdentifyFieldUsagePass.class);
    var isaMatching = ((HashMap<InstructionLabel, List<Instruction>>) passResults.lastResultOf(
        IsaMatchingPass.class));
    var immediates = ((List<TableGenImmediateRecord>) passResults.lastResultOf(
        GenerateTableGenImmediateRecordPass.class));

    var additionWithImmediates = isaMatching.getOrDefault(InstructionLabel.ADDI_64,
        isaMatching.getOrDefault(InstructionLabel.ADDI_32, Collections.emptyList()));
    ensure(!additionWithImmediates.isEmpty(), () -> Diagnostic.error(
        "Specification has no instruction for addition with immediate. Therefore, vadl "
            + "cannot generate an instruction for constant materialisation.",
        viam.sourceLocation()));
    var addi = additionWithImmediates.stream().findFirst().get();

    for (var imm : immediates) {
      var name = addi.identifier.append(imm.rawName() + "_const_mat");
      var copy = addi.behavior().copy();
      var graph = setupGraph(fieldUsages, name, copy, addi);
      var parameters = createParameters(copy);
      var instruction =
          new ConstantMatPseudoInstruction(name,
              parameters, graph, addi.assembly(), imm);
      constantMatInstructions.add(instruction);
    }

    return constantMatInstructions;
  }

  private Parameter[] createParameters(Graph copy) {
    var rawRd = ensurePresent(
        copy.getNodes(WriteRegFileNode.class).map(WriteRegFileNode::address).findFirst(),
        () -> Diagnostic.error("Destination register write must exist.", copy.sourceLocation()));
    ensure(rawRd instanceof FieldRefNode,
        () -> Diagnostic.error("Address of register write must be a field",
            rawRd.sourceLocation()));
    FieldRefNode rd = (FieldRefNode) rawRd;

    var imm =
        ensurePresent(copy.getNodes(FieldRefNode.class).filter(fieldRefNode -> fieldRefNode.usages()
                    .allMatch(
                        usage -> !(usage instanceof WriteResourceNode
                            || usage instanceof ReadResourceNode)))
                .findFirst(),
            () -> Diagnostic.error("Immediate required for instruction", copy.sourceLocation())
        );

    return new Parameter[] {new Parameter(rd.formatField().identifier, rd.type()),
        new Parameter(imm.formatField().identifier, imm.type())};
  }

  private Graph setupGraph(IdentifyFieldUsagePass.ImmediateDetectionContainer fieldUsages,
                           Identifier name, Graph copy, Instruction addi) {
    var pseudoInstructionGraph = new Graph(name.toString());
    var arguments =
        new ArrayList<ExpressionNode>();

    var fieldsNodes = copy.getNodes(FieldRefNode.class).toList();
    var immField = ensurePresent(fieldsNodes
            .stream()
            .filter(fieldNode -> fieldNode.usages().noneMatch(
                usage -> usage instanceof ReadResourceNode || usage instanceof WriteResourceNode))
            .findFirst(),
        () -> Diagnostic.error("Expected immediate in instruction", addi.sourceLocation())
    );

    // The order of the parameters is important.
    // In the loop, we construct the arguments. We set the source registers to the zero registers.
    for (var fieldRefNode : fieldsNodes) {
      if (fieldRefNode == immField) {
        arguments.add(
            new FuncParamNode(
                new Parameter(fieldRefNode.formatField().identifier, fieldRefNode.type())));
      } else {
        var registerUsages = fieldUsages.getRegisterUsages(addi);
        var registerUsage = registerUsages.get(fieldRefNode.formatField());
        ensureNonNull(registerUsage,
            () -> Diagnostic.error("Could not detect how the register field is used.",
                    fieldRefNode.sourceLocation())
                .note("A register field can be used as source, destination or both.")
        );

        switch (Objects.requireNonNull(registerUsage)) {
          case BOTH ->
              throw Diagnostic.error("Register field cannot be used as source and destination.",
                  fieldRefNode.sourceLocation()).build();
          case DESTINATION -> {
            arguments.add(
                new FuncParamNode(
                    new Parameter(fieldRefNode.formatField().identifier, fieldRefNode.type())));
          }
          case SOURCE -> {
            var zeroConstraint = extractZeroConstraintFromRegisterFile(fieldRefNode);
            var addressOfZeroRegister = zeroConstraint.address().intValue();
            var registerArgument = new ConstantNode(
                Constant.Value.of(addressOfZeroRegister, zeroConstraint.address().type()));
            arguments.add(registerArgument);
          }
          default -> throw new RuntimeException("not implementedEmitMCExprCppFilePass");
        }
      }
    }


    var instrCallNode =
        new InstrCallNode(addi, fieldsNodes.stream().map(FieldRefNode::formatField).toList(),
            new NodeList<ExpressionNode>(arguments));
    var start = new StartNode(instrCallNode);
    pseudoInstructionGraph.addWithInputs(start);
    pseudoInstructionGraph.addWithInputs(instrCallNode);

    return pseudoInstructionGraph;
  }

  /**
   * We use the addition with immediate instruction to generate constant materialisation.
   * For that we set the source registers to zero. However, we need a zero register for that.
   * This method tries to find such a constraint and throws an error if cannot find it.
   */
  private static RegisterFile.@NotNull Constraint extractZeroConstraintFromRegisterFile(
      FieldRefNode fieldRefNode) {
    var registerFile = ensurePresent(fieldRefNode.usages()
        .filter(x -> x instanceof HasRegisterFile)
        .map(x -> ((HasRegisterFile) x).registerFile())
        .findFirst(), () -> Diagnostic.error(
        "Cannot find a register file. This field is not used as a register index.",
        fieldRefNode.sourceLocation()));
    return ensurePresent(Arrays.stream(registerFile.constraints())
        .filter(constraint -> constraint.value().intValue() == 0)
        .findFirst(), () -> Diagnostic.error(
            "It is required that the register file has a zero constraint at some address.",
            registerFile.sourceLocation())
        .note(
            "The compiler generator creates a constant materialisation pseudo "
                + "instruction which uses the underlying addition with immediate machine "
                +
                "instruction. It tries to set the source register to zero, but was not able "
                + "to do so because a zero register is missing.")
    );
  }
}
