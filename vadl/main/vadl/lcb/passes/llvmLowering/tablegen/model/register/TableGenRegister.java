package vadl.lcb.passes.llvmLowering.tablegen.model.register;

import java.util.List;
import java.util.stream.Collectors;
import vadl.gcb.valuetypes.ProcessorName;

/**
 * Represents a single register in TableGen.
 */
public record TableGenRegister(ProcessorName namespace,
                               String name,
                               String asmName,
                               List<String> altNames,
                               int hwEncodingMsb,
                               int hwEncodingValue) {

  public String altNamesString() {
    return String.join(", ", altNames);
  }
}
