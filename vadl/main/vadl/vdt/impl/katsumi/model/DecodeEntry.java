package vadl.vdt.impl.katsumi.model;

import java.util.Set;
import vadl.vdt.utils.BitPattern;
import vadl.vdt.utils.Instruction;

public class DecodeEntry extends Instruction {

  private final Set<ExclusionCondition> exclusionConditions;

  public DecodeEntry(vadl.viam.Instruction source, int width, BitPattern pattern,
                     Set<ExclusionCondition> exclusionConditions) {
    super(source, width, pattern);
    this.exclusionConditions = exclusionConditions;
  }

  public Set<ExclusionCondition> exclusionConditions() {
    return exclusionConditions;
  }
}
