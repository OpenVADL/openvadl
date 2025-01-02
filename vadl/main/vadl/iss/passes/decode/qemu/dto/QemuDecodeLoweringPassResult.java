package vadl.iss.passes.decode.qemu.dto;

import java.util.List;

/**
 * Nested record for the result of the QEMU decode pass. Contains a list of patterns, each
 * representing an instruction, containing the format, argument sets, and fields extracted
 * from the instruction encoding.
 *
 * @param argSets  The list of argument sets resolved from the QEMU decode lowering pass
 * @param formats  The list of formats resolved from the QEMU decode lowering pass
 * @param patterns The list of patterns resolved from the QEMU decode lowering pass
 */
public record QemuDecodeLoweringPassResult(List<ArgumentSet> argSets, List<Format> formats,
                                           List<Pattern> patterns) {
}
