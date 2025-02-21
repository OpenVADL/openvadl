package vadl.lcb.passes.llvmLowering.tablegen.model.register;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.template.Renderable;

/**
 * Represents a single register in TableGen.
 */
public record TableGenRegister(ProcessorName namespace,
                               String name,
                               String asmName,
                               List<String> altNames,
                               int hwEncodingMsb,
                               int hwEncodingValue,
                               Optional<Integer> index) implements Renderable {

  public String altNamesString() {
    return String.join(", ", altNames);
  }

  @Override
  public Map<String, Object> renderObj() {
    var map = new HashMap<String, Object>();
    map.put("namespace", namespace);
    map.put("name", name);
    map.put("asmName", asmName);
    map.put("altNames", altNames);
    map.put("hwEncodingMsb", hwEncodingMsb);
    map.put("hwEncodingValue", hwEncodingValue);
    map.put("altNamesString", altNamesString());
    index.ifPresent(integer -> map.put("index", integer));
    return map;
  }
}
