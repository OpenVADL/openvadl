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

package vadl.viam;

import com.google.errorprone.annotations.concurrent.LazyInit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.viam.graph.Graph;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.ReadStageOutputNode;
import vadl.viam.graph.dependency.WriteResourceNode;
import vadl.viam.graph.dependency.WriteStageOutputNode;

/**
 * Stage definition in MiA description.
 *
 * <p>A stage has a behavior and outputs.
 */
public class Stage extends Definition implements DefProp.WithBehavior {

  @LazyInit
  @SuppressWarnings("unused")
  private MicroArchitecture mia;

  private Graph behavior;

  private final List<StageOutput> outputs;

  private @Nullable Stage prev;

  private @Nullable List<Stage> next;

  /**
   * Instantiate a new stage definition.
   *
   * @param identifier stage identifier
   * @param behavior   behavior graph
   * @param outputs    list of stage outputs
   */
  public Stage(Identifier identifier, Graph behavior, List<StageOutput> outputs) {
    super(identifier);
    this.behavior = behavior;
    this.outputs = new ArrayList<>(outputs);

    this.behavior.setParentDefinition(this);
  }

  public MicroArchitecture mia() {
    return mia;
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

  /**
   * Get all resources read by this stage.
   *
   * @return list of resources
   */
  public List<Resource> resourceReads() {
    return behavior.getNodes(ReadResourceNode.class)
        .map(ReadResourceNode::resourceDefinition)
        .toList();
  }

  /**
   * Get all resources writte by this stage.
   *
   * @return list of resources
   */
  public List<Resource> resourceWrites() {
    return behavior.getNodes(WriteResourceNode.class)
        .map(WriteResourceNode::resourceDefinition)
        .toList();
  }

  /**
   * Get all stage output definitions used by this stage as inputs.
   *
   * @return list of stage outputs
   */
  public List<StageOutput> inputs() {
    return behavior.getNodes(ReadStageOutputNode.class)
        .map(ReadStageOutputNode::stageOutput)
        .toList();
  }

  public List<StageOutput> outputs() {
    return outputs;
  }

  public void addOutput(StageOutput output) {
    outputs.add(output);
  }

  public void removeOutput(StageOutput output) {
    outputs.remove(output);
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
    return "Stage{ name='" + simpleName() + "', sourceLocation=" + location() + "}";
  }

  @Override
  public void verify() {
    super.verify();
    behavior.verify();

    var writes = behavior.getNodes(WriteStageOutputNode.class)
        .map(WriteStageOutputNode::stageOutput)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    outputs.forEach(output -> ensure(writes.contains(output),
        "Output %s is not written to", output.simpleName()));
  }
}
