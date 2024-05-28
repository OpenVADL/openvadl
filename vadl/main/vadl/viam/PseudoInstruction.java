package vadl.viam;

import java.util.List;
import java.util.stream.Stream;
import vadl.viam.graph.Graph;
import vadl.viam.graph.dependency.FuncParamNode;


/**
 * Represents a pseudo instruction in a VADL specification.
 *
 * <p>The behavior graph must fulfill certain condition to be valid and can be checked
 * using the {@link Graph#isPseudoInstruction()} method. The most
 * important graph node to handle is the {@link vadl.viam.graph.control.InstrCallNode}.</p>
 */
public class PseudoInstruction extends Definition {

  private final List<Parameter> parameters;
  private final Graph behavior;

  /**
   * Instantiates a PseudoInstruction object and verifies it.
   *
   * @param identifier the identifier of the pseudo instruction
   * @param parameters the list of parameters for the pseudo instruction
   * @param behavior   the behavior graph of the pseudo instruction
   */
  public PseudoInstruction(
      Identifier identifier,
      List<Parameter> parameters,
      Graph behavior
  ) {
    super(identifier);

    this.parameters = parameters;
    this.behavior = behavior;

    verify();
  }

  public Stream<Parameter> parameters() {
    return parameters.stream();
  }

  public Graph behavior() {
    return behavior;
  }

  @Override
  public void verify() {
    ensure(behavior.isPseudoInstruction(),
        "The given behavior is not a valid pseudo instruction behaviour");

    behavior.getNodes(FuncParamNode.class)
        .forEach(node ->
            node.ensure(parameters.contains(node.parameter()),
                "The given parameter is not a known pseudo instruction parameter")
        );
  }
}
