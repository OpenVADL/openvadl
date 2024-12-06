package vadl.lcb.passes.isaMatching.database;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import vadl.lcb.passes.isaMatching.MachineInstructionLabel;
import vadl.lcb.passes.isaMatching.PseudoInstructionLabel;
import vadl.viam.graph.Node;

public class Query {
  @Nullable
  private MachineInstructionLabel machineInstructionLabel;

  @Nullable
  private PseudoInstructionLabel pseudoInstructionLabel;

  private final List<BehaviorQuery> withBehavior;

  private final List<Query> or;

  public Query(@Nullable MachineInstructionLabel machineInstructionLabel,
               @Nullable PseudoInstructionLabel pseudoInstructionLabel,
               List<Query> or,
               List<BehaviorQuery> withBehavior) {
    this.machineInstructionLabel = machineInstructionLabel;
    this.pseudoInstructionLabel = pseudoInstructionLabel;
    this.withBehavior = withBehavior;
    this.or = or;
  }

  @Nullable
  public MachineInstructionLabel machineInstructionLabel() {
    return machineInstructionLabel;
  }

  @Nullable
  public PseudoInstructionLabel pseudoInstructionLabel() {
    return pseudoInstructionLabel;
  }

  public List<Query> or() {
    return or;
  }

  public List<BehaviorQuery> withBehavior() {
    return withBehavior;
  }

  public static class Builder {

    @Nullable
    private MachineInstructionLabel machineInstructionLabel;

    @Nullable
    private PseudoInstructionLabel pseudoInstructionLabel;

    private final List<Query> or = new ArrayList<>();

    private final List<BehaviorQuery> withBehavior = new ArrayList<>();

    public Builder machineInstructionLabel(MachineInstructionLabel machineInstructionLabel) {
      this.machineInstructionLabel = machineInstructionLabel;
      return this;
    }

    public Builder pseudoInstructionLabel(PseudoInstructionLabel pseudoInstructionLabel) {
      this.pseudoInstructionLabel = pseudoInstructionLabel;
      return this;
    }

    public Builder or(Query query) {
      this.or.add(query);
      return this;
    }

    public Builder withBehavior(BehaviorQuery b) {
      this.withBehavior.add(b);
      return this;
    }

    public Query build() {
      return new Query(machineInstructionLabel, pseudoInstructionLabel, or, withBehavior);
    }
  }
}
