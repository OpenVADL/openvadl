package vadl.lcb.template.utils;

import static vadl.viam.ViamError.ensureNonNull;

import java.util.stream.Stream;
import vadl.error.DeferredDiagnosticStore;
import vadl.error.Diagnostic;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.pass.PassResults;
import vadl.viam.Instruction;
import vadl.viam.PseudoInstruction;
import vadl.viam.Specification;
import vadl.viam.graph.control.InstrCallNode;

/**
 * Utility class for getting {@link PseudoInstruction}.
 */
public class PseudoInstructionProvider {
  /**
   * Get the list of {@link PseudoInstruction} which only contain {@link Instruction} which
   * are lowered to LLVM.
   */
  public static Stream<PseudoInstruction> getSupportedPseudoInstructions(
      Specification specification,
      PassResults passResults) {
    var supportedInstructions = ensureNonNull(
        (LlvmLoweringPass.LlvmLoweringPassResult) passResults.lastResultOf(LlvmLoweringPass.class),
        "llvmLoweringPass result must exist").machineInstructionRecords()
        .keySet();
    return specification.isa()
        .map(x -> x.ownPseudoInstructions().stream()).orElseGet(Stream::empty)
        .filter(pseudoInstruction -> pseudoInstruction.behavior().getNodes(InstrCallNode.class)
            .allMatch(i -> {
              var isSupported = supportedInstructions.contains(i.target());
              if (!isSupported) {
                DeferredDiagnosticStore.add(Diagnostic.warning(
                    "Instruction was not lowered. "
                        + "Therefore, it cannot be used in the pseudo instruction",
                    i.sourceLocation()).build());
              }
              return isSupported;
            }));
  }
}
