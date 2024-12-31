package vadl.iss.passes.decode.dto;

import java.util.Collection;

/**
 * The result of the QEMU decode resolve symbol pass, containing the resolved patterns, formats,
 * argument sets and fields.
 *
 * @param patterns The resolved patterns
 * @param formats  The resolved formats
 * @param argSets  The resolved argument sets
 * @param fields   The resolved fields
 */
public record QemuDecodeResolveSymbolPassResult(Collection<Pattern> patterns,
                                                Collection<Format> formats,
                                                Collection<ArgumentSet> argSets,
                                                Collection<Field> fields) {
}
