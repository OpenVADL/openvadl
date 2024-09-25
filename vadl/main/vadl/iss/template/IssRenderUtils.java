package vadl.iss.template;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;
import vadl.cppCodeGen.CppTypeMap;
import vadl.viam.Register;
import vadl.viam.RegisterFile;
import vadl.viam.Specification;

public class IssRenderUtils {
  public static Map<String, Object> map(RegisterFile rf) {
    var size = (int) Math.pow(2, rf.addressType().bitWidth());
    var name = rf.identifier.simpleName();
    return Map.of(
        "name", name,
        "name_upper", name.toUpperCase(),
        "name_lower", name.toLowerCase(),
        "size", String.valueOf(size),
        "value_width", rf.resultType().bitWidth(),
        "value_c_type", CppTypeMap.getCppTypeNameByVadlType(rf.resultType()),
        "names", IntStream.range(0, size)
            .mapToObj(i -> name + i)
            .toList(),
        "constraints", Arrays.stream(rf.constraints())
            .map(c -> Map.of(
                "index", c.address().intValue(),
                "value", c.value().intValue()
            )).toList()
    );
  }

  public static Map<String, String> map(Register reg) {
    return Map.of(
        "name", reg.identifier.simpleName(),
        "name_upper", reg.identifier.simpleName().toUpperCase(),
        "name_lower", reg.identifier.simpleName().toLowerCase(),
        "c_type", CppTypeMap.getCppTypeNameByVadlType(reg.type())
    );
  }

  public static List<Map<String, String>> mapRegs(Specification spec) {
    return spec.isa().get()
        .ownRegisters()
        .stream().map(IssRenderUtils::map)
        .toList();
  }

  public static List<Map<String, Object>> mapRegFiles(Specification spec) {
    return spec.isa().get()
        .ownRegisterFiles()
        .stream().map(IssRenderUtils::map)
        .toList();
  }

  public static Map<String, String> mapPc(Specification spec) {
    var pcReg = (Register) Objects.requireNonNull(spec.isa().get()
        .pc()).registerResource();

    return map(pcReg);
  }

}
