package vadl.lcb.passes.isaMatching.database;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import vadl.lcb.passes.isaMatching.MachineInstructionLabel;
import vadl.lcb.passes.isaMatching.PseudoInstructionLabel;
import vadl.viam.ViamError;

/**
 * Query to find instructions and pseudo instructions.
 */
public class Query {
  @Nullable
  private MachineInstructionLabel machineInstructionLabel;

  @Nullable
  private PseudoInstructionLabel pseudoInstructionLabel;

  private final List<BehaviorQuery> withBehavior;

  private final List<Query> or;

  /**
   * Constructor.
   */
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

  /**
   * Getter.
   */
  public List<Query> or() {
    return or;
  }

  /**
   * Getter.
   */
  public List<BehaviorQuery> withBehavior() {
    return withBehavior;
  }

  /**
   * Builder for the Query.
   */
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

    public Builder machineInstructionLabels(Collection<MachineInstructionLabel> labels) {
      if (labels.isEmpty()) {
        return this;
      }

      this.machineInstructionLabel = ViamError.unwrap(labels.stream().findFirst());
      var rest = labels.stream().skip(1).toList();

      if (!rest.isEmpty()) {
        var subQuery = new Builder().machineInstructionLabels(rest);
        or.add(subQuery.build());
      }

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
