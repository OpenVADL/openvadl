package vadl.lcb.passes.llvmLowering.strategies.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import vadl.lcb.passes.isaMatching.InstructionLabel;
import vadl.lcb.passes.llvmLowering.model.LlvmFrameIndexSD;
import vadl.lcb.passes.llvmLowering.strategies.LlvmLoweringStrategy;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionFrameRegisterOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionRegisterFileOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.types.BuiltInTable;
import vadl.viam.Instruction;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.WriteResourceNode;
import vadl.viam.matching.TreeMatcher;
import vadl.viam.matching.impl.AnyReadRegFileMatcher;
import vadl.viam.matching.impl.BuiltInMatcher;
import vadl.viam.matching.impl.FieldAccessRefMatcher;
import vadl.viam.matching.impl.WriteResourceMatcherForAddr;
import vadl.viam.passes.functionInliner.UninlinedGraph;

/**
 * Lowers instructions which can store into memory.
 */
public class LlvmLoweringMemStoreStrategyImpl extends LlvmLoweringStrategy {

  @Override
  protected Set<InstructionLabel> getSupportedInstructionLabels() {
    return Set.of(InstructionLabel.STORE_MEM);
  }

  @Override
  protected List<TableGenPattern> generatePatternVariations(
      Instruction instruction,
      Map<InstructionLabel, List<Instruction>> supportedInstructions,
      InstructionLabel instructionLabel,
      UninlinedGraph behavior,
      List<TableGenInstructionOperand> inputOperands,
      List<TableGenInstructionOperand> outputOperands,
      List<TableGenPattern> patterns) {
    ArrayList<TableGenPattern> alternatives = new ArrayList<>();
    replaceAddrWithFrameIndexRegister(instruction, behavior, inputOperands, alternatives);

    return alternatives;
  }

  private void replaceAddrWithFrameIndexRegister(Instruction instruction,
                                                 UninlinedGraph behavior,
                                                 List<TableGenInstructionOperand> inputOperands,
                                                 ArrayList<TableGenPattern> alternatives) {
    /*

   def : Pat<(i32 (load (add X:$rs1, RV32Zicsr_Itype_ImmediateI_immediateAsInt32:$imm))),
          (LW X:$rs1, RV32Zicsr_Itype_ImmediateI_immediateAsInt32:$imm)>;

    to

   def : Pat<(i32 (load (add AddrFI:$rs1, RV32Zicsr_Itype_ImmediateI_immediateAsInt32:$imm))),
          (LW AddrFI:$rs1, RV32Zicsr_Itype_ImmediateI_immediateAsInt32:$imm)>;


   AddrFI is a special pattern which is also a register class but only one register (frame pointer)
   is allowed.
     */

    var copy = behavior.copy();
    var matcher = new WriteResourceMatcherForAddr(new BuiltInMatcher(
        List.of(BuiltInTable.ADD, BuiltInTable.ADDS, BuiltInTable.ADDC),
        List.of(new AnyReadRegFileMatcher(),
            new FieldAccessRefMatcher())));

    var matches = TreeMatcher.matches(copy.getNodes(), matcher);
    for (var match : matches) {
      var casted = (WriteResourceNode) match;
      var castedBuiltin = (BuiltInCall) casted.address();
      var regFileNode = (ReadRegFileNode) Objects.requireNonNull(castedBuiltin).arguments().get(0);
      regFileNode.replaceAndDelete(new LlvmFrameIndexSD(regFileNode));

      if (regFileNode.address() instanceof FieldRefNode addr) {
        var updatedMachineOperands = new ArrayList<>(inputOperands);
        var oldRef = updatedMachineOperands.stream()
            .filter(x -> x instanceof TableGenInstructionRegisterFileOperand op
                && op.formatField() == addr.formatField()).findFirst();
        if (oldRef.isPresent()) {
          var newRef = new TableGenInstructionFrameRegisterOperand(oldRef.get().name());
          updatedMachineOperands.set(updatedMachineOperands.indexOf(oldRef.get()), newRef);
          alternatives.addAll(generatePatterns(instruction, updatedMachineOperands,
              copy.getNodes(WriteResourceNode.class).map(x -> (WriteResourceNode) x.copy())
                  .toList()));
        }
      }
    }
  }
}
