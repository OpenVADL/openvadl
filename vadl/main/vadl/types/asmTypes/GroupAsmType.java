package vadl.types.asmTypes;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/**
 * GroupAsmType is a special AsmType that contains a list of subtypes.
 * It represents the type of a sequence of grammar elements.
 */
public class GroupAsmType implements AsmType {

  Map<String, AsmType> subtypeMap;

  public GroupAsmType(Map<String, AsmType> subtypeMap) {
    this.subtypeMap = subtypeMap;
  }

  public Map<String, AsmType> getSubtypeMap() {
    return subtypeMap;
  }

  @Override
  public String name() {
    return "GroupAsmType";
  }

  @Override
  public String toCppTypeString(String prefix) {
    if (subtypeMap.isEmpty()) {
      return "NoData";
    }
    return subtypeMap.keySet().stream()
        .reduce("", (acc, type) -> acc + type);
  }

  @Override
  public boolean canBeCastTo(AsmType to) {

    var subtypes = subtypeMap.values().stream().toList();

    if (subtypes.size() == 1 && subtypes.get(0) == to) {
      return true;
    }

    if (to == InstructionAsmType.instance()) {
      return subtypes.stream().allMatch(subtype -> subtype == OperandAsmType.instance());
    }

    if (to == OperandAsmType.instance()) {
      return subtypes.size() == 2 && subtypes.get(0) == ModifierAsmType.instance()
          && subtypes.get(1) == ExpressionAsmType.instance();
    }

    if (to == StatementsAsmType.instance()) {
      return subtypes.stream().allMatch(subtype -> subtype == InstructionAsmType.instance());
    }

    if (to == OperandsAsmType.instance()) {
      return subtypes.stream().allMatch(subtype -> subtype == OperandAsmType.instance());
    }

    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    GroupAsmType that = (GroupAsmType) o;
    return Arrays.equals(subtypeMap.values().toArray(), that.subtypeMap.values().toArray());
  }

  @Override
  public int hashCode() {
    return Objects.hash(subtypeMap.values());
  }

  @Override
  public String toString() {
    var nameWithSubtypes = new StringBuilder("@").append(name()).append("(");

    subtypeMap.values().forEach(sub -> nameWithSubtypes.append(sub.toString()).append(", "));
    nameWithSubtypes.replace(nameWithSubtypes.length() - 2, nameWithSubtypes.length() - 1, ")");

    return nameWithSubtypes.toString();
  }
}
