package vadl.rtl.passes;

import java.util.List;
import vadl.rtl.ipg.InstructionProgressGraph;
import vadl.viam.DefProp;
import vadl.viam.Definition;
import vadl.viam.DefinitionExtension;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.graph.Graph;

/**
 * Definition extension to attached to the instruction set architecture containing the
 * instruction progress graph.
 */
public class InstructionProgressGraphExtension
    extends DefinitionExtension<InstructionSetArchitecture>
    implements DefProp.WithBehavior {

  private final InstructionProgressGraph ipg;

  public InstructionProgressGraphExtension(InstructionProgressGraph ipg) {
    this.ipg = ipg;
  }

  public InstructionProgressGraph ipg() {
    return ipg;
  }

  @Override
  public Class<? extends Definition> extendsDefClass() {
    return InstructionSetArchitecture.class;
  }

  @Override
  public List<Graph> behaviors() {
    return List.of(ipg);
  }
}
