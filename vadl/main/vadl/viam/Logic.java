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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.types.Type;
import vadl.utils.Pair;
import vadl.viam.graph.Graph;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.ProcEndNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;
import vadl.viam.graph.dependency.WriteSignalNode;

/**
 * Logic definition in MiA description.
 */
public abstract class Logic extends Definition implements DefProp.WithBehavior {

  @LazyInit
  @SuppressWarnings("unused")
  private MicroArchitecture mia;

  private final List<Signal> signals;
  private final List<RegisterTensor> registers;

  private final Graph behavior;

  /**
   * Create new empty logic element.
   *
   * @param identifier identifier of the logic element
   */
  public Logic(Identifier identifier) {
    super(identifier);
    this.signals = new ArrayList<>();
    this.registers = new ArrayList<>();
    this.behavior = new Graph(identifier.simpleName(), this);

    // dummy graph with minimal control flow
    var end = new ProcEndNode(new NodeList<>());
    var start = new StartNode(end);
    this.behavior.add(end);
    this.behavior.add(start);
  }

  public MicroArchitecture mia() {
    return mia;
  }

  public void setMia(MicroArchitecture mia) {
    this.mia = mia;
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    return identifier.simpleName() + ": " + getClass().getSimpleName();
  }

  public List<Signal> signals() {
    return signals;
  }

  public void addSignal(Signal signal) {
    signals.add(signal);
  }

  public List<RegisterTensor> registers() {
    return registers;
  }

  public void addRegister(RegisterTensor register) {
    registers.add(register);
  }

  public Graph behavior() {
    return behavior;
  }

  @Override
  public List<Graph> behaviors() {
    return Collections.singletonList(behavior);
  }

  @Override
  public void verify() {
    super.verify();
    behavior.verify();

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

  /**
   * Logic definition for control logic (created by MiA synthesis).
   */
  public static class Control extends Logic {

    private final Map<Stage, Signal> enable = new HashMap<>();

    public Control(Identifier identifier) {
      super(identifier);
    }

    /**
     * Get or create an enable signal for the given stage.
     *
     * @param stage stage
     * @return enable signal
     */
    public Signal getEnable(Stage stage) {
      return enable.computeIfAbsent(stage, s -> {
        var sig = new Signal(identifier.append(s.simpleName() + "_en"), Type.bool());
        signals().add(sig);
        return sig;
      });
    }

  }

  /**
   * Logic definition for a forwarding unit.
   */
  public static class Forwarding extends Logic {

    private final Map<ReadResourceNode, Signal> enable = new HashMap<>();

    private final Map<Pair<ReadResourceNode, Stage>, Signal> enableFrom = new HashMap<>();

    public Forwarding(Identifier identifier) {
      super(identifier);
    }

    /**
     * Add a forward enable signal for a read node to the forwarding logic.
     *
     * @param node read node
     * @param signal forward enable signal
     */
    public void putEnable(ReadResourceNode node, Signal signal) {
      enable.put(node, signal);
      if (!signals().contains(signal)) {
        signals().add(signal);
      }
    }

    /**
     * Get the forward enable signal for a read node.
     *
     * @param node read node
     * @return forward enable signal
     */
    @Nullable
    public Signal getEnable(ReadResourceNode node) {
      return enable.get(node);
    }

    /**
     * Add a forward enable signal for a read node and a source stage to the forwarding logic.
     *
     * @param node read node
     * @param stage stage forwarding from
     * @param signal forward enable signal for read node and source stage
     */
    public void putEnableFrom(ReadResourceNode node, Stage stage, Signal signal) {
      enableFrom.put(Pair.of(node, stage), signal);
      if (!signals().contains(signal)) {
        signals().add(signal);
      }
    }

    /**
     * Get the forward enable signal for a read node and a source stage.
     *
     * @param node read node
     * @param stage stage forwarding from
     * @return forward enable signal for read node and source stage
     */
    @Nullable
    public Signal getEnableFrom(ReadResourceNode node, Stage stage) {
      return enableFrom.get(Pair.of(node, stage));
    }

  }

  /**
   * Logic definition for a branch predictor.
   */
  public static class BranchPrediction extends Logic {

    public BranchPrediction(Identifier identifier) {
      super(identifier);
    }

  }

  /**
   * Logic definition for RVFI logic for formal verification.
   */
  public static class RVFI extends Logic {

    public RVFI(Identifier identifier) {
      super(identifier);
    }

    /**
     * Get signals the RVFI logic outputs.
     *
     * @return signals this logic writes to but not contains.
     */
    public List<Signal> outputSignals() {
      return behavior().getNodes(WriteSignalNode.class)
          .map(WriteSignalNode::signal)
          .filter(signals()::contains)
          .toList();
    }

  }
}
