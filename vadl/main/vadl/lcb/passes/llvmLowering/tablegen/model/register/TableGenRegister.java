package vadl.lcb.passes.llvmLowering.tablegen.model.register;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import vadl.gcb.valuetypes.CompilerRegister;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.template.Renderable;

/**
 * Represents a single register in TableGen.
 */
public record TableGenRegister(ProcessorName namespace,
                               CompilerRegister compilerRegister,
                               int hwEncodingMsb,
                               Optional<Integer> index) implements Renderable {

  public String altNamesString() {
    return String.join(", ",
        compilerRegister.altNames().stream().map(x -> "\"" + x + "\"").toList());
  }

  @Override
  public Map<String, Object> renderObj() {
    var map = new HashMap<String, Object>();
    map.put("namespace", namespace);
    map.put("name", compilerRegister.name());
    map.put("asmName", compilerRegister.asmName());
    map.put("dwarfNumber", compilerRegister.dwarfNumber());
    map.put("hwEncodingMsb", hwEncodingMsb);
    map.put("hwEncodingValue", compilerRegister.hwEncodingValue());
    map.put("altNamesString", altNamesString());
    index.ifPresent(integer -> map.put("index", integer));
    return map;
  }
}
