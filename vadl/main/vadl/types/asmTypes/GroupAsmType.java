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

  public List<AsmType> getSubtypes() {
    return subtypes;
  }

  @Override
  public String name() {
    return "GroupAsmType";
  }

  @Override
  public boolean canBeCastTo(AsmType to) {

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
