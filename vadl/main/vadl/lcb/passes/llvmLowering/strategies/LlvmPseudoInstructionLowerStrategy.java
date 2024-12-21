package vadl.lcb.passes.llvmLowering.strategies;

import com.google.common.collect.Streams;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.error.DeferredDiagnosticStore;
import vadl.error.Diagnostic;
import vadl.gcb.passes.pseudo.PseudoFuncParamNode;
import vadl.lcb.passes.isaMatching.MachineInstructionLabel;
import vadl.lcb.passes.isaMatching.PseudoInstructionLabel;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.domain.LlvmLoweringPseudoRecord;
import vadl.lcb.passes.llvmLowering.domain.LlvmLoweringRecord;
import vadl.lcb.passes.llvmLowering.domain.RegisterRef;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionNode;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbPseudoInstructionNode;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenSelectionWithOutputPattern;
import vadl.utils.Pair;
import vadl.viam.Instruction;
import vadl.viam.PseudoInstruction;
import vadl.viam.graph.Graph;
import vadl.viam.graph.HasRegisterFile;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.WriteRegFileNode;
import vadl.viam.Abi;

/**
 * Defines a {@link PseudoInstruction} will be lowered to {@link TableGenInstruction}.
 */
public abstract class LlvmPseudoInstructionLowerStrategy {
  /**
   * We use the strategies from {@link LlvmLoweringPass} for the individual
   * {@link Instruction} from {@link InstrCallNode} in {@link PseudoInstruction}.
   */
  private final List<LlvmInstructionLoweringStrategy> strategies;

  /**
   * Constructor.
   */
  protected LlvmPseudoInstructionLowerStrategy(List<LlvmInstructionLoweringStrategy> strategies) {
    this.strategies = strategies;
  }

  /**
   * Get the supported set of {@link PseudoInstructionLabel} which this strategy supports.
   */
  protected abstract Set<PseudoInstructionLabel> getSupportedInstructionLabels();

  /**
   * Checks whether the given {@link PseudoInstruction} is lowerable with this strategy.
   */
  public boolean isApplicable(@Nullable PseudoInstructionLabel pseudoInstructionLabel) {
    if (pseudoInstructionLabel == null) {
      return false;
    }

    return getSupportedInstructionLabels().contains(pseudoInstructionLabel);
  }

  /**
   * Lower a {@link PseudoInstruction} into a {@link LlvmLoweringPseudoRecord}.
   */
  public Optional<LlvmLoweringPseudoRecord> lower(
      Abi abi,
      PseudoInstruction pseudo,
      Map<MachineInstructionLabel, List<Instruction>> labelledMachineInstructions) {
    var patterns = new ArrayList<TableGenPattern>();
    var flippedInstructions =
        LlvmLoweringPass.flipMachineInstructions(labelledMachineInstructions);

    var uses = new ArrayList<RegisterRef>();
    var defs = new ArrayList<RegisterRef>();
    var inputOperands = new ArrayList<TableGenInstructionOperand>();
    var outputOperands = new ArrayList<TableGenInstructionOperand>();

    var isTerminator = false;
    var isReturn = false;
    var mayLoad = false;
    var mayStore = false;
    var isBranch = false;

    if (pseudo.behavior().getNodes(InstrCallNode.class).toList().size() > 1) {
      DeferredDiagnosticStore.add(
          Diagnostic.warning(
              "Cannot generate instruction selectors for pseudo instruction with multiple "
                  + "machine instructions",
              pseudo.sourceLocation()).build());
    }
    // This variable keeps track of the graph which has the applied arguments.
    // Key: Instruction
    // Value: Graph with applied arguments
    var appliedInstructionBehavior = new IdentityHashMap<Instruction, Graph>();

    for (var callNode : pseudo.behavior().getNodes(InstrCallNode.class).toList()) {
      var instructionBehavior = callNode.target().behavior().copy();
      appliedInstructionBehavior.put(callNode.target(), instructionBehavior);

      /*
      Example:
      pseudo instruction RET =
      {
          JALR{ rs1 = 1 as Bits5, rd = 0 as Bits5, imm = 0 as Bits12 }
      }
      */

      // Apply the argument from pseudo instruction.
      Streams.zip(callNode.getParamFields().stream(), callNode.arguments().stream(),
              Pair::new)
          .forEach(app -> {
            var formatField = app.left();
            var argument = indexArgument(callNode.arguments(), app.right());

            /*
              pseudo instruction MOV( rd : Index, rs1 : Index ) =
              {
                  ADDI{ rd = rd, rs1 = rs1, imm = 0 as Bits12 }
              }
             */
            Stream.concat(
                    instructionBehavior.getNodes(FieldRefNode.class),
                    instructionBehavior.getNodes(FieldAccessRefNode.class)
                )
                .filter(x -> {
                  if (x instanceof FieldRefNode fieldRefNode) {
                    return fieldRefNode.formatField().equals(formatField);
                  } else if (x instanceof FieldAccessRefNode fieldAccessRefNode) {
                    return fieldAccessRefNode.fieldAccess().fieldRef().equals(formatField);
                  }
                  return false;
                })
                .forEach(occurrence -> {
                  // Edge case:
                  // When we have the following pseudo instruction. Note that "r1" is replaced
                  // by a constant. Sometimes, we need to create instruction selectors in TableGen,
                  // and it requires a variable. However, if we replace the field by a constant
                  // we lose the name of the variable because we have no field anymore.
                  // {
                  //     JALR{ rs1 = 1 as Bits5, rd = 0 as Bits5, imm = 0 as Bits12 }
                  // }

                  if (argument instanceof ConstantNode constantNode) {
                    // The constantNode tells me that it will be used as a register index.

                    // Go over the usages to emit warnings.
                    // We need the usage because we need to find out what the register file
                    // to check for constraints.
                    occurrence.usages().filter(node -> (node instanceof HasRegisterFile))
                        .forEach(node -> {
                          var cast = (HasRegisterFile) node;

                          var constraintValue =
                              Arrays.stream(cast.registerFile().constraints()).filter(
                                  c -> c.address().intValue()
                                      == constantNode.constant().asVal().intValue()).findFirst();

                          if (constraintValue.isEmpty()) {
                            DeferredDiagnosticStore.add(Diagnostic.warning(
                                "There is no constraint value for this register. "
                                    +
                                    "Therefore, we cannot generate instruction selectors for it.",
                                occurrence.sourceLocation()).build());
                          }
                        });

                    occurrence.replaceAndDelete(argument.copy());

                    // After the replacement, we can check whether we have a write node with
                    // constant node as address which has a constraint. If that's the case, then we
                    // can remove the side effect.
                    occurrence.usages()
                        .filter(node -> node instanceof WriteRegFileNode writeRegFileNode
                            && writeRegFileNode.hasConstantAddress()
                            // Check if there is a constraint for this register index.
                            && Arrays.stream(writeRegFileNode.registerFile().constraints())
                            .anyMatch(constraint -> constraint.address().intValue()
                                == constantNode.constant().asVal().intValue()))
                        .forEach(Node::safeDelete);
                  } else {
                    occurrence.replaceAndDelete(argument.copy());
                  }
                });
          });

      var label = flippedInstructions.get(callNode.target());

      // Skip not supported instructions
      if (label == null) {
        continue;
      }

      for (var strategy : strategies) {
        if (!strategy.isApplicable(label)) {
          continue;
        }

        var tableGenRecord = strategy.lower(labelledMachineInstructions,
            pseudo,
            callNode.target(),
            instructionBehavior,
            abi);

        if (tableGenRecord.isPresent()) {
          var record = tableGenRecord.get();

          updatePatterns(pseudo, record);
          final var patternVariations =
              generatePatternVariations(pseudo, record, appliedInstructionBehavior);

          var flags = record.flags();
          isTerminator |= flags.isTerminator();
          isReturn |= flags.isReturn();
          mayLoad |= flags.mayLoad();
          mayStore |= flags.mayStore();
          isBranch |= flags.isBranch();
          defs.addAll(record.defs());
          uses.addAll(record.uses());
          inputOperands.addAll(record.inputs());
          outputOperands.addAll(record.outputs());
          patterns.addAll(record.patterns());
          patterns.addAll(patternVariations);
        }

        break;
      }
    }

    var flags = new LlvmLoweringPass.Flags(isTerminator,
        isBranch,
        false,
        isReturn,
        true,
        false,
        mayLoad,
        mayStore);

    return Optional.of(new LlvmLoweringPseudoRecord(pseudo.behavior(),
        dedup(inputOperands),
        dedup(outputOperands),
        flags,
        patterns,
        dedup(uses),
        dedup(defs),
        appliedInstructionBehavior
    ));
  }

  private <T> List<T> dedup(
      List<T> x) {
    return new ArrayList<>(new LinkedHashSet<>(x));
  }

  protected List<TableGenPattern> generatePatternVariations(
      PseudoInstruction pseudo,
      LlvmLoweringRecord record,
      IdentityHashMap<Instruction, Graph> appliedInstructionBehavior) {
    return Collections.emptyList();
  }

  protected void updatePatterns(PseudoInstruction pseudo, LlvmLoweringRecord record) {
    // We need to update the output instruction because the pattern has the machine
    // instruction now. But we want the pseudo instruction.
    record.patterns().forEach(pattern -> {
      if (pattern instanceof TableGenSelectionWithOutputPattern outputPattern) {
        outputPattern.machine().getNodes(LcbMachineInstructionNode.class)
            .forEach(machineInstructionNode -> machineInstructionNode.replaceAndDelete(
                new LcbPseudoInstructionNode(machineInstructionNode.arguments(), pseudo)
            ));
      }
    });
  }

  /**
   * There are two relevant cases.
   * The first is that the {@code argument} is a constant. Then, we do not have to do anything.
   * The second case is when {@link PseudoInstruction} uses an {@code index}. Then, the argument
   * is replaced by a {@link FuncParamNode}. However, we still require to know the index for
   * the pseudo instance expansion. That's why we extend {@link FuncParamNode} with
   * {@link PseudoFuncParamNode} which has an {@code index} property.
   * Here is an example of the index. Note that {@code rs} will be transformed into
   * a {@link PseudoFuncParamNode} when it is replaced.
   * <code>
   * pseudo instruction BGEZ( rs : Index, offset : Bits<12> ) =
   * {
   * BGE{ rs1 = rs, rs2 = 0 as Bits5, imm = offset }
   * }
   * </code>
   */
  protected ExpressionNode indexArgument(List<ExpressionNode> arguments, ExpressionNode argument) {
    if (argument instanceof FuncParamNode funcParamNode) {
      int index = arguments.indexOf(argument);
      return new PseudoFuncParamNode(funcParamNode.parameter(), index);
    }
    return argument;
  }
}
