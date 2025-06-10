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
import java.util.List;
import vadl.viam.graph.Graph;

/**
 * Logic definition in MiA description.
 */
public abstract class Logic extends Definition implements DefProp.WithBehavior {

  @LazyInit
  @SuppressWarnings("unused")
  private MicroArchitecture mia;

  private final List<Signal> signals;
  private final List<RegisterTensor> registers;

  private Graph behavior;

  public Logic(Identifier identifier) {
    super(identifier);
    this.signals = new ArrayList<>();
    this.registers = new ArrayList<>();
    this.behavior = new Graph(identifier.simpleName());
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

  /**
   * Logic definition for control logic (created by MiA synthesis).
   */
  public static class Control extends Logic {

    public Control(Identifier identifier) {
      super(identifier);
    }

  }

  /**
   * Logic definition for a forwarding unit.
   */
  public static class Forwarding extends Logic {

    public Forwarding(Identifier identifier) {
      super(identifier);
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
}
