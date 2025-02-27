package vadl.viam.graph.dependency;

import vadl.javaannotations.viam.DataValue;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.Logic;
import vadl.viam.Resource;
import vadl.viam.graph.NodeList;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a VADL micro architecture built-in process.
 *
 * <p>Holds optional lists of resources and logic elements for reference in instruction built-ins,
 * for example {@code @X}, {@code @bypass} in {@code instruction.readOrForward(@X, @bypass)}.
 *
 * @see vadl.types.BuiltInTable
 */
public class MiaBuiltInCall extends BuiltInCall {

  @DataValue
  private final List<Resource> resources;

  @DataValue
  private final List<Logic> logic;

  public MiaBuiltInCall(BuiltInTable.BuiltIn builtIn, NodeList<ExpressionNode> args, Type type) {
    super(builtIn, args, type);
    this.resources = new ArrayList<>();
    this.logic = new ArrayList<>();
    ensure(BuiltInTable.MIA_BUILTINS.contains(builtIn), "Not a micro architecture builtin");
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(resources);
    collection.add(logic);
  }

  public List<Resource> resources() {
    return resources;
  }

  public void add(Resource resource) {
    resources.add(resource);
  }

  public List<Logic> logic() {
    return logic;
  }

  public void add(Logic logic) {
    this.logic.add(logic);
  }

}
