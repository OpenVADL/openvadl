package vadl.iss.passes.decode.dto;

import java.util.Collection;

public record QemuDecodeResolveSymbolPassResult(Collection<Pattern> patterns,
                                                Collection<Format> formats,
                                                Collection<ArgumentSet> argSets,
                                                Collection<Field> fields) {
}
