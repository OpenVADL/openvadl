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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.viam.graph.Graph;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.ReadStageOutputNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;
import vadl.viam.graph.dependency.WriteResourceNode;
import vadl.viam.graph.dependency.WriteSignalNode;
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

  private final List<Signal> signals;

  private final List<RegisterTensor> registers;

  private final Set<String> localNames;

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
    this.signals = new ArrayList<>();
    this.registers = new ArrayList<>();
    this.localNames = new HashSet<>();

    // TODO this should be handled by the frontend (only add stage outputs from definition)
    // currently the passed stage output list is always empty, it could contain the list of
    // outputs give in the spec
    this.behavior.getNodes(WriteStageOutputNode.class)
        .map(WriteStageOutputNode::stageOutput).forEach(output -> {
          if (!this.outputs.contains(output)) {
            this.outputs.add(output);
          }
        });

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
   * Get all resources written by this stage.
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
    localNames.add(output.simpleName());
  }

  public void removeOutput(StageOutput output) {
    outputs.remove(output);
    localNames.remove(output.simpleName());
  }

  public List<Signal> signals() {
    return signals;
  }

  public void addSignal(Signal signal) {
    signals.add(signal);
    localNames.add(signal.simpleName());
  }

  public void removeSignal(Signal signal) {
    signals.remove(signal);
    localNames.remove(signal.simpleName());
  }

  public List<RegisterTensor> registers() {
    return registers;
  }

  public void addRegister(RegisterTensor register) {
    registers.add(register);
    localNames.add(register.simpleName());
  }

  public void removeRegister(RegisterTensor register) {
    registers.remove(register);
    localNames.remove(register.simpleName());
  }

  public Set<String> localNames() {
    return localNames;
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

    var outputWrites = behavior.getNodes(WriteStageOutputNode.class)
        .map(WriteStageOutputNode::stageOutput)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    outputs.forEach(output -> ensure(outputWrites.contains(output),
        "Output %s is not written to", output.simpleName()));

    var signalWrites = behavior.getNodes(WriteSignalNode.class)
        .map(WriteSignalNode::signal)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    signals.forEach(signal -> ensure(signalWrites.contains(signal),
        "Signal %s is not written to", signal.simpleName()));

    var registerWrites = behavior.getNodes(WriteRegTensorNode.class)
        .map(WriteRegTensorNode::registerTensor)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    registers.forEach(regTensor -> ensure(registerWrites.contains(regTensor),
        "Register %s is not written to", regTensor.simpleName()));
  }
}
