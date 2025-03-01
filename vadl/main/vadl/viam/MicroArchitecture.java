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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * A Micro architecture (MiA) definition of a VADL specification.
 */
public class MicroArchitecture extends Definition {

  private final MicroProcessor processor;

  // Stages and Logic elements
  private final List<Stage> stages;
  private final List<Logic> logic;

  // Resources
  private final List<Signal> signals;
  private final List<Register> registers;
  private final List<RegisterFile> registerFiles;
  private final List<Memory> memories;

  private final List<Function> functions;

  /**
   * Create a micro architecture definition.
   *
   * @param identifier identifier
   * @param processor micro processor definition
   * @param stages list of stages
   * @param logic list of logic elements
   */
  public MicroArchitecture(Identifier identifier, MicroProcessor processor, List<Stage> stages,
                           List<Logic> logic) {
    this(identifier, processor, stages, logic, new ArrayList<>(), new ArrayList<>(),
        new ArrayList<>(),
        new ArrayList<>(), new ArrayList<>());
  }

  /**
   * Create a micro architecture definition.
   *
   * @param identifier identifier
   * @param processor micro processor definition
   * @param stages list of stages
   * @param logic list of logic elements
   * @param signals list of signals
   * @param registers list of registers
   * @param registerFiles list of register files
   * @param memories list of memories
   * @param functions list of functions
   */
  public MicroArchitecture(Identifier identifier, MicroProcessor processor, List<Stage> stages,
                           List<Logic> logic, List<Signal> signals, List<Register> registers,
                           List<RegisterFile> registerFiles, List<Memory> memories,
                           List<Function> functions) {
    super(identifier);
    this.processor = processor;
    this.stages = stages;
    this.logic = logic;
    this.signals = signals;
    this.registers = registers;
    this.registerFiles = registerFiles;
    this.memories = memories;
    this.functions = functions;

    for (Stage stage : stages) {
      stage.setMia(this);
    }
    for (Logic l : logic) {
      l.setMia(this);
    }
  }

  public MicroProcessor processor() {
    return processor;
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

  public List<Register> ownRegisters() {
    return registers;
  }

  public List<RegisterFile> ownRegisterFiles() {
    return registerFiles;
  }

  public List<Memory> ownMemories() {
    return memories;
  }

  public List<Function> ownFunctions() {
    return functions;
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }
}
