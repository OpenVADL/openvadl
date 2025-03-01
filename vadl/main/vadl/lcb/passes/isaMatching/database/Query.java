// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.lcb.passes.isaMatching.database;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import vadl.lcb.passes.isaMatching.MachineInstructionLabel;
import vadl.lcb.passes.isaMatching.MachineInstructionLabelGroup;
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

  @Nullable
  private MachineInstructionLabelGroup machineInstructionLabelGroup;

  private final List<BehaviorQuery> withBehavior;

  private final List<Query> or;

  /**
   * Constructor.
   */
  public Query(@Nullable MachineInstructionLabel machineInstructionLabel,
               @Nullable PseudoInstructionLabel pseudoInstructionLabel,
               List<Query> or,
               @Nullable MachineInstructionLabelGroup machineInstructionLabelGroup,
               List<BehaviorQuery> withBehavior) {
    this.machineInstructionLabel = machineInstructionLabel;
    this.pseudoInstructionLabel = pseudoInstructionLabel;
    this.withBehavior = withBehavior;
    this.or = or;
    this.machineInstructionLabelGroup = machineInstructionLabelGroup;
  }

  @Nullable
  public MachineInstructionLabel machineInstructionLabel() {
    return machineInstructionLabel;
  }

  @Nullable
  public PseudoInstructionLabel pseudoInstructionLabel() {
    return pseudoInstructionLabel;
  }

  @Nullable
  public MachineInstructionLabelGroup machineInstructionLabelGroup() {
    return machineInstructionLabelGroup;
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
    private MachineInstructionLabelGroup machineInstructionLabelGroup;

    @Nullable
    private MachineInstructionLabel machineInstructionLabel;

    @Nullable
    private PseudoInstructionLabel pseudoInstructionLabel;

    private final List<Query> or = new ArrayList<>();

    private final List<BehaviorQuery> withBehavior = new ArrayList<>();

    /**
     * Set a machine instruction label group.
     */
    public Builder machineInstructionLabelGroup(MachineInstructionLabelGroup group) {
      this.machineInstructionLabelGroup = group;
      return this;
    }

    /**
     * Set the machine instruction label.
     */
    public Builder machineInstructionLabel(MachineInstructionLabel machineInstructionLabel) {
      this.machineInstructionLabel = machineInstructionLabel;
      return this;
    }

    /**
     * Set the machine instructions labels.
     */
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
      return new Query(machineInstructionLabel, pseudoInstructionLabel, or,
          machineInstructionLabelGroup, withBehavior);
    }
  }
}
