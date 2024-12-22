package vadl.types.asmTypes;

import java.util.List;
import java.util.Objects;

/**
 * GroupAsmType is a special AsmType that contains a list of subtypes.
 * It represents the type of a sequence of grammar elements.
 */
public class GroupAsmType implements AsmType {

  List<AsmType> subtypes;

  public GroupAsmType(List<AsmType> subtypes) {
    this.subtypes = subtypes;
  }

  @Override
  public String name() {
    return "GroupAsmType";
  }

  @Override
  public boolean canBeCastTo(AsmType to) {

    if (to == InstructionAsmType.instance()) {
      return subtypes.stream().allMatch(subtype -> subtype == OperandAsmType.instance());
    }

    if (to == OperandAsmType.instance()) {
      if (subtypes.size() != 2) {
        return false;
      }
      return subtypes.get(0) == ModifierAsmType.instance()
          && subtypes.get(1) == ExpressionAsmType.instance();
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
    if (subtypes.size() != that.subtypes.size()) {
      return false;
    }

    boolean equal = true;
    for (int i = 0; i < subtypes.size(); i++) {
      equal &= subtypes.get(0).equals(that.subtypes.get(0));
    }
    return equal;
  }

  @Override
  public int hashCode() {
    return Objects.hash(subtypes);
  }

  @Override
  public String toString() {
    var nameWithSubtypes = new StringBuilder("@").append(name()).append("(");

    for (int i = 0; i < subtypes.size(); i++) {
      nameWithSubtypes.append(subtypes.get(i));
      if (i != subtypes.size() - 1) {
        nameWithSubtypes.append(", ");
      }
    }
    nameWithSubtypes.append(")");

    return nameWithSubtypes.toString();
  }
}
