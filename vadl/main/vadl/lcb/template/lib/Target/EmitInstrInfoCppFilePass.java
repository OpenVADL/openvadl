package vadl.lcb.template.lib.Target;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import vadl.configuration.LcbConfiguration;
import vadl.lcb.passes.isaMatching.InstructionLabel;
import vadl.lcb.passes.isaMatching.IsaMatchingPass;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Instruction;
import vadl.viam.RegisterFile;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegFileNode;

/**
 * This file contains the logic for adjusting registers in instructions.
 */
public class EmitInstrInfoCppFilePass extends LcbTemplateRenderingPass {

    public EmitInstrInfoCppFilePass(LcbConfiguration lcbConfiguration)
        throws IOException {
        super(lcbConfiguration);
    }

    @Override
    protected String getTemplatePath() {
        return "lcb/llvm/lib/Target/InstrInfo.cpp";
    }

    @Override
    protected String getOutputPath() {
        var processorName = lcbConfiguration().processorName().value();
        return "llvm/lib/Target/" + processorName + "/" + processorName
            + "InstrInfo.cpp";
    }

    /**
     * An {@link Instruction} for copying a register.
     *
     * @param instruction      is the machine instruction which does the copying.
     * @param srcRegisterFile  is the register file for the source register in LLVM.
     * @param destRegisterFile is the register file for the destination register in LLVM.
     */
    record CopyPhysRegInstruction(Instruction instruction, RegisterFile srcRegisterFile,
                                  RegisterFile destRegisterFile) {
    }

    /**
     * An {@link Instruction} for storing on the stack.
     *
     * @param instruction      is the machine instruction which does the copying.
     * @param destRegisterFile is the register file for the destination register in LLVM.
     * @param words            indicates how many words are stored.
     */
    record StoreRegSlot(Instruction instruction, RegisterFile destRegisterFile, int words) {

    }

    private List<CopyPhysRegInstruction> getMovInstructions(
        HashMap<InstructionLabel, List<Instruction>> isaMatching) {
        var addi32 = mapWithInstructionLabel(InstructionLabel.ADDI_32, isaMatching);
        var addi64 = mapWithInstructionLabel(InstructionLabel.ADDI_64, isaMatching);

        return Stream.concat(addi32.stream(), addi64.stream()).toList();
    }

    private List<StoreRegSlot> getStoreMemoryInstructions(
        HashMap<InstructionLabel, List<Instruction>> isaMatching) {
        var instructions = (List<Instruction>) isaMatching.getOrDefault(InstructionLabel.STORE_MEM,
            Collections.emptyList());

        var mapped = instructions.stream()
            .map(i -> {
                var destRegisterFile =
                    ensurePresent(i.behavior().getNodes(ReadRegFileNode.class).findFirst(),
                        "There must be destination register").registerFile();
                var words =
                    ensurePresent(i.behavior().getNodes(WriteMemNode.class).findFirst(),
                        "There must be a write mem node").words();
                return new StoreRegSlot(i, destRegisterFile, words);
            })
            // Sort by largest word size descending
            .sorted((storeRegSlot, t1) -> Integer.compare(t1.words, storeRegSlot.words))
            .toList();

        return mapped;
    }

    private List<CopyPhysRegInstruction> mapWithInstructionLabel(
        InstructionLabel label,
        HashMap<InstructionLabel, List<Instruction>> isaMatching) {
        var instructions = (List<Instruction>) isaMatching.getOrDefault(label, Collections.emptyList());

        return instructions.stream()
            .map(i -> {
                var destRegisterFile =
                    ensurePresent(i.behavior().getNodes(WriteRegFileNode.class).findFirst(),
                        "There must be destination register").registerFile();
                var srcRegisterFile =
                    ensurePresent(i.behavior().getNodes(ReadRegFileNode.class).findFirst(),
                        "There must be source register").registerFile();

                return new CopyPhysRegInstruction(i, srcRegisterFile, destRegisterFile);
            })
            .toList();
    }

    @Override
    protected Map<String, Object> createVariables(final PassResults passResults,
                                                  Specification specification) {
        var isaMatches = (HashMap<InstructionLabel, List<Instruction>>) passResults.lastResultOf(
            IsaMatchingPass.class);
        return Map.of(CommonVarNames.NAMESPACE, specification.name(),
            "copyPhysInstructions", getMovInstructions(isaMatches),
            "storeStackSlotInstructions", getStoreMemoryInstructions(isaMatches)
        );
    }
}
