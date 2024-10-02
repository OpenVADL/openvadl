package vadl.lcb.passes.llvmLowering.tablegen.model.register;

import java.util.List;
import vadl.gcb.valuetypes.ProcessorName;

/**
 * Represents a single register in TableGen.
 */
public record TableGenRegister(ProcessorName namespace,
                               String registerName,
                               String asmName,
                               List<String> altNames,
                               int hwEncodingMsb,
                               int hwEncodingValue) {
}
