// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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

import static java.util.Objects.requireNonNull;

import com.google.errorprone.annotations.concurrent.LazyInit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * A Micro architecture (MiA) definition of a VADL specification.
 */
public class MicroArchitecture extends Definition {

  private final InstructionSetArchitecture instructionSetArchitecture;

  // Stages and Logic elements
  private final List<Stage> stages;
  private final List<Logic> logic;

  // Resources
  private final List<Signal> signals;
  private final List<RegisterTensor> registers;
  private final List<Memory> memories;

  private final List<Function> functions;
  private final List<Operation> operations;

  // Dependencies between MiA elements (stages and logic).
  private final List<MiaDependency> allDependencies;
  private final HashMap<Definition, List<MiaDependency>> dependenciesBySource;
  private final HashMap<Definition, List<MiaDependency>> dependenciesByDestination;

  private @LazyInit Stage rootStage;

  /**
   * Create a micro architecture definition.
   *
   * @param identifier                 identifier
   * @param instructionSetArchitecture processor definition
   * @param stages                     list of stages
   * @param logic                      list of logic elements
   * @param signals                    list of signals
   * @param registers                  list of registers (tensors)
   * @param memories                   list of memories
   * @param functions                  list of functions
   * @param operations                 list of operations
   */
  public MicroArchitecture(
      Identifier identifier,
      InstructionSetArchitecture instructionSetArchitecture,
      List<Stage> stages,
      List<Logic> logic,
      List<Signal> signals,
      List<RegisterTensor> registers,
      List<Memory> memories,
      List<Function> functions,
      List<Operation> operations
  ) {
    super(identifier);

    this.instructionSetArchitecture = instructionSetArchitecture;
    this.stages = stages;
    this.logic = logic;
    this.signals = signals;
    this.registers = registers;
    this.memories = memories;
    this.functions = functions;
    this.operations = operations;
    this.allDependencies = new ArrayList<>();
    this.dependenciesBySource = new HashMap<>();
    this.dependenciesByDestination = new HashMap<>();

    for (Stage stage : stages) {
      stage.setMia(this);
    }

    for (Logic l : logic) {
      l.setMia(this);
    }
  }

  public InstructionSetArchitecture isa() {
    return instructionSetArchitecture;
  }

  /**
   * A list containing all the {@link MiaDependency MiaDependencies} in the
   * MiA.
   *
   * @return A list of all {@code MiaDependencies}.
   *
   * @see #dependenciesBySource()
   * @see #dependenciesByDestination()
   */
  public List<MiaDependency> allDependencies() {
    return allDependencies;
  }

  /**
   * A map from {@link Definition Definitions} to lists of
   * {@link MiaDependency MiaDependencies}. Each list contains all the
   * dependencies for which the key {@link Definition} is found as the
   * {@link MiaDependency#source() source}.
   *
   * @return A map from {@code Definitions} to lists of {@code MiaDependencies}
   *         containing them in their {@link MiaDependency#source() source}
   *         field.
   *
   * @see #allDependencies()
   * @see #dependenciesBySource()
   */
  public Map<Definition, List<MiaDependency>> dependenciesBySource() {
    return dependenciesBySource;
  }

  /**
   * A map from {@link Definition Definitions} to lists of
   * {@link MiaDependency MiaDependencies}. Each list contains all the
   * dependencies for which the key {@link Definition} is found as the
   * {@link MiaDependency#destination() destination}.
   *
   * @return A map from {@code Definitions} to lists of {@code MiaDependencies}
   *         containing them in their
   *         {@link MiaDependency#destination() destination} field.
   *
   * @see #allDependencies()
   * @see #dependenciesBySource()
   */
  public Map<Definition, List<MiaDependency>> dependenciesByDestination() {
    return dependenciesByDestination;
  }

  /**
   * The one stage that has no inputs, i.e., is never returned by
   * {@link MiaDependency#destination()}. This is useful as a starting point
   * for traversals of the MiA's dependency graph.
   *
   * <p>The root stage is guaranteed to be unique. If there are multiple stages
   * that would qualify as roots, an error is issued during creation of the
   * MiA.
   *
   * @return The single stage that has no inputs.
   */
  public Stage rootStage() {
    return requireNonNull(rootStage);
  }

  /**
   * Sets the root stage for future retrieval by {@link #rootStage()}.
   *
   * <p>This method may only be called once during construction of the VIAM.
   *
   * @param rootStage The MiA's root stage.
   */
  public void setRootStage(Stage rootStage) {
    if (this.rootStage != null) {
      throw new IllegalStateException("Tried setting MiA's root stage more than once.");
    }

    this.rootStage = requireNonNull(rootStage);
  }

  public List<Stage> stages() {
    return stages;
  }

  /**
   * Set stage order according to given list of stages. This sets the prev/next references of the
   * stage definitions.
   *
   * @param stages list of stages in correct order
   */
  public void setStageOrder(List<Stage> stages) {
    var eq =
        this.stages.size() == stages.size() && new HashSet<>(stages).containsAll(this.stages);
    ensure(eq, "Ordered stages list must contain all stages and not more");
    this.stages.clear();
    this.stages.addAll(stages);
    for (int i = 1; i < stages.size(); i++) {
      stages.get(i).setPrev(stages.get(i - 1));
      stages.get(i - 1).setNext(Collections.singletonList(stages.get(i)));
    }
  }

  public List<Logic> logic() {
    return logic;
  }

  public List<Signal> signals() {
    return signals;
  }

  public List<RegisterTensor> ownRegisters() {
    return registers;
  }

  public List<Memory> ownMemories() {
    return memories;
  }

  public List<Function> ownFunctions() {
    return functions;
  }

  public List<Operation> ownOperations() {
    return operations;
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }
}
