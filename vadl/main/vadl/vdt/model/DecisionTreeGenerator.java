package vadl.vdt.model;

import java.util.Collection;
import vadl.viam.Instruction;

public interface DecisionTreeGenerator {

  Node generateTree(Collection<Instruction> instructions);

}
