package vadl.vdt.impl.irregular.model;

import java.util.Set;
import vadl.vdt.utils.BitPattern;

public record ExclusionCondition(BitPattern matching, Set<BitPattern> unmatching) {
}
