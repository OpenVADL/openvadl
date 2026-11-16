package vadl.lcb.passes.registerExtensionsTruncations;

import static vadl.viam.ViamError.ensurePresent;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import vadl.configuration.GeneralConfiguration;
import vadl.error.Diagnostic;
import vadl.gcb.passes.MachineInstructionLabel;
import vadl.gcb.valuetypes.CompilerRegister;
import vadl.gcb.valuetypes.CompilerRegister.SubRegIndex;
import vadl.gcb.valuetypes.ValueType;
import vadl.lcb.passes.isaMatching.IsaMachineInstructionMatchingPass;
import vadl.lcb.passes.llvmLowering.GenerateTableGenRegistersPass;
import vadl.lcb.passes.llvmLowering.tablegen.model.register.TableGenRegisterClass;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.ArtificialResource;
import vadl.viam.Constant;
import vadl.viam.Instruction;
import vadl.viam.RegisterResource;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.WriteResourceNode;

/**
 * This pass generates patterns between a register class and its subregister class. For example, in Aarch64
 * a {@code sext} from a {@code W} subregister to a {@code X} superregister is lowered to a shift right arithmetic nested with a shift left logical.
 */
public class GenerateRegisterExtensionTruncationPatterns extends Pass {
    
    public GenerateRegisterExtensionTruncationPatterns(GeneralConfiguration configuration){
        super(configuration);
    }

    @Override
    public PassName getName() {
        return new PassName ("GenerateRegisterExtensionTruncationPatterns");
    }

    @Override
    public RegisterSubregisterPatterns execute(final PassResults passResults, Specification viam) {

        var generateTableGenRegisterPassOutput =
        ((GenerateTableGenRegistersPass.Output) passResults.lastResultOf(
            GenerateTableGenRegistersPass.class));
        var registerFiles =
            Stream.concat(generateTableGenRegisterPassOutput.registerClasses().stream(),
                    generateTableGenRegisterPassOutput.aliasRegisterClasses().stream())
                .toList();
        var labelledMachineInstructions = ((IsaMachineInstructionMatchingPass.Result)
            passResults.lastResultOf(IsaMachineInstructionMatchingPass.class)).labels();

        return generateRegisterSubregisterPatterns(registerFiles, labelledMachineInstructions);
    }

    private RegisterSubregisterPatterns generateRegisterSubregisterPatterns(
        List<TableGenRegisterClass> registerClasses,
        Map<MachineInstructionLabel, List<Instruction>> labelledMachineInstructions){
            var patterns = new RegisterSubregisterPatterns(
                new HashSet<>(),
                new HashSet<>(),
                new HashSet<>(),
                new HashSet<>());
        
        registerClasses
            .forEach(regClass -> {
                var regs = regClass.registers().getFirst();
                this.generateMovePatternsForAllSubregisters(
                    regClass,
                    regs.subRegs(),
                    regs.subRegIndices(),
                    patterns,
                    labelledMachineInstructions);
            });
        return patterns;
    }
    
    private void generateMovePatternsForAllSubregisters(
        TableGenRegisterClass current,
        List<CompilerRegister> subRegs,
        List<SubRegIndex> subRegsIndices,
        RegisterSubregisterPatterns patterns,
        Map<MachineInstructionLabel, List<Instruction>> labelledMachineInstructions) {

        if (subRegs.isEmpty()) {
        return;
        }

        var currentType = current.registerFileRef().type().asDataType();
        var currentWidth = currentType.bitWidth();

        for (int i = 0; i < subRegs.size(); i++) {
        var subReg = subRegs.get(i);
        var subRegResource = subReg.registerFile();
        var subType = subRegResource.type();
        var subWidth = subType.asDataType().bitWidth();
        var subSliceRange = this.getRegisterSliceRange(subRegResource);
        var subIdx = subRegsIndices.get(i).name();
        var rawAnd = labelledMachineInstructions.getOrDefault(MachineInstructionLabel.AND, List.of());
        var rawSll = labelledMachineInstructions.getOrDefault(MachineInstructionLabel.SLL, List.of());
        var rawAsr = labelledMachineInstructions.getOrDefault(MachineInstructionLabel.ASR, List.of());

        if (subSliceRange.lsb() != 0) {
            continue;
        }

        if (subWidth < currentWidth) {

            long mask = (1L << subWidth) -1;
            long shiftAmount = (long)currentWidth - subWidth;

            var and = instructionByWidth(rawAnd, currentWidth, subRegResource);
            var sll = instructionByWidth(rawSll, currentWidth, subRegResource);
            var asr = instructionByWidth(rawAsr, currentWidth, subRegResource);

            /* Zero-extension has 2 patterns depending on annotation. In Aarch64 for example, the W subregister class
            has a [overwrite source : zero] annotation, therefore the pattern containing the SUBREG_TO_REG LLVM psuedoinstruction is emitted.*/
            boolean upperBitsZero = subRegResource instanceof ArtificialResource ar &&
                    ar.semantics().overwriteMode() == ArtificialResource.OverwriteMode.ZERO;
                    
            if (and != null){
                patterns.zeroExtensions().add(new RegisterSubregisterZeroExtensionMove(
                    subRegResource.simpleName(),
                    ValueType.from(currentType).get().getLlvmType(),
                    subIdx,
                    and.simpleName(),
                    upperBitsZero,
                    mask));
            }

            if (asr != null && sll !=null){
                patterns.signExtensions().add(new RegisterSubregisterSignExtensionMove(
                    subRegResource.simpleName(),
                    ValueType.from(currentType).get().getLlvmType(),
                    subIdx,
                    asr.simpleName(),
                    sll.simpleName(),
                    shiftAmount));
            }

            patterns.truncations().add(new RegisterSubregisterMove(
                current.name(), 
                ValueType.from(subType).get().getLlvmType(),
                subRegsIndices.get(i).name()));

            patterns.anyExtensions().add(new RegisterSubregisterMove(
                subRegResource.simpleName(),
                ValueType.from(currentType).get().getLlvmType(),
                subRegsIndices.get(i).name()));
        }

        this.generateMovePatternsForAllSubregisters(
                current,
                subReg.subRegs(),
                subReg.subRegIndices(),
                patterns,
                labelledMachineInstructions);
        }
    }

    private Instruction instructionByWidth(List<Instruction> instructions,
        int width,
        RegisterResource registerResource) {

        var instruction = instructions.stream()
            .filter(instr -> instr.behavior()
                .getNodes(WriteResourceNode.class)
                .anyMatch(node -> node.writeBitWidth() == width))
            .findFirst();

        return ensurePresent(instruction, () -> Diagnostic.error(
            "Instruction set requires a " + width + "-bit " + instruction,
            registerResource.location()));
    }


    private Constant.BitSlice.Part getRegisterSliceRange(RegisterResource r) {
        if (r instanceof ArtificialResource ar) {
        return Optional.ofNullable(ar.aliasSlice())
            .flatMap(s -> s.parts().findFirst())
            .orElseGet(() -> new Constant.BitSlice.Part(ar.type().asDataType().bitWidth(), 0));
        }

        return new Constant.BitSlice.Part(r.type().asDataType().bitWidth(), 0);
    }

    public record RegisterSubregisterPatterns(
        Set<RegisterSubregisterMove> truncations,
        Set<RegisterSubregisterMove> anyExtensions,
        Set<RegisterSubregisterZeroExtensionMove> zeroExtensions,
        Set<RegisterSubregisterSignExtensionMove> signExtensions){
    }

    public record RegisterSubregisterMove(
        String register,
        String resultType,
        String subIdx){
    }

    public record RegisterSubregisterZeroExtensionMove(
            String register,
            String resultType,
            String subIdx,
            String and,
            Boolean upperBitsZero,
            long mask){
    }

    public record RegisterSubregisterSignExtensionMove(
            String register,
            String resultType,
            String subIdx,
            String asr,
            String sll,
            long shiftAmount){
    }

}
