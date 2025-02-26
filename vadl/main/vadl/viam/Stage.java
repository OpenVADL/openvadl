package vadl.viam;

import com.google.errorprone.annotations.concurrent.LazyInit;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import vadl.viam.graph.Graph;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.WriteResourceNode;

/**
 * Stage definition in MiA description.
 *
 * <p>A stage has a behavior and outputs (assignments to the outputs are write resource nodes).
 */
public class Stage extends Definition implements DefProp.WithBehavior {

  @LazyInit
  @SuppressWarnings("unused")
  private MicroArchitecture mia;

  private Graph behavior;

  private final List<Resource> inputs;

  private final List<Resource> outputs;

  private @Nullable Stage prev;

  private @Nullable List<Stage> next;

  public Stage(Identifier identifier, Graph behavior, List<Resource> inputs,
               List<Resource> outputs) {
    super(identifier);
    this.behavior = behavior;
    this.inputs = new ArrayList<>(inputs);
    this.outputs = new ArrayList<>(outputs);

    this.behavior.setParentDefinition(this);
  }

  public void setMia(MicroArchitecture mia) {
    this.mia = mia;
  }

  public Graph behavior() {
    return behavior;
  }

  public void setBehavior(Graph behavior) {
    this.behavior = behavior;
  }

  public List<Resource> inputs() {
    return inputs;
  }

  public void addInput(Resource resource) {
    inputs.add(resource);
  }

  public List<Resource> reads() {
    return behavior.getNodes(ReadResourceNode.class)
        .map(ReadResourceNode::resourceDefinition)
        .toList();
  }

  public List<Resource> writes() {
    return behavior.getNodes(WriteResourceNode.class)
        .map(WriteResourceNode::resourceDefinition)
        .toList();
  }

  public List<Resource> outputs() {
    return outputs;
  }

  public void addOutput(Resource resource) {
    outputs.add(resource);
  }

  public void removeOutput(Resource resource) {
    outputs.remove(resource);
  }

  @Override
  public List<Graph> behaviors() {
    return List.of(behavior);
  }

  @Nullable
  public Stage prev() {
    return prev;
  }

  public void setPrev(@Nullable Stage prev) {
    this.prev = prev;
  }

  @Nullable
  public List<Stage> next() {
    return next;
  }

  public void setNext(@Nullable List<Stage> next) {
    this.next = next;
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    return "Stage{ name='" + simpleName() + "', sourceLocation=" + sourceLocation() + "}";
  }

  @Override
  public void verify() {
    super.verify();
    behavior.verify();

    var writes = writes();
    outputs.forEach(output ->
            ensure(writes.contains(output), "Output %s is not written to", output.simpleName()));
  }
}
