package vadl.dump.infoEnrichers;

import static vadl.dump.InfoEnricher.forType;

import java.util.ArrayList;
import java.util.List;
import vadl.dump.Info;
import vadl.dump.InfoEnricher;
import vadl.dump.InfoUtils;
import vadl.dump.entities.DefinitionEntity;
import vadl.iss.passes.IssTcgAnnotatePass;
import vadl.viam.DefProp;
import vadl.viam.Instruction;
import vadl.viam.graph.Node;

/**
 * A collection of info enrichers that provide information during the ISS generation.
 */
public class IssEnricherCollection {


  /**
   * A list of all info enrichers that are ISS specific.
   */
  public static List<InfoEnricher> all = List.of(

  );

}
