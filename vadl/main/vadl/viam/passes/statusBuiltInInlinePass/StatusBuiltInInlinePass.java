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

package vadl.viam.passes.statusBuiltInInlinePass;

import java.io.IOException;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.VadlBuiltInStatusOnlyDispatcher;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.dependency.BuiltInCall;

/**
 * This pass inlines all status built-ins,
 * so the behaviors do not contain any status results afterward.
 * E.g., when a graph contains a call to {@link vadl.types.BuiltInTable#ADDS}, it will
 * be replaced by built-ins that compute the status flags returned by ADDS.
 *
 * <p>This pass will only create logic for status flags if they are used by the callee.
 * E.g., if the {@code status.negative} flag is not used, no logic for this will be created.</p>
 */
public class StatusBuiltInInlinePass extends Pass {

  public StatusBuiltInInlinePass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("Status BuiltIn Inline Pass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var isa = viam.isa().get();
    isa.ownInstructions().forEach(i ->
        new StatusBuiltInInliner(i.behavior()).run());

    return null;
  }
}

/**
 * Inlines all status built-ins for the given graph.
 * There is a {@link Inliner} for each status built-in.
 */
class StatusBuiltInInliner implements VadlBuiltInStatusOnlyDispatcher<BuiltInCall> {

  private final Graph graph;

  StatusBuiltInInliner(Graph graph) {
    this.graph = graph;
  }

  void run() {
    graph.getNodes(BuiltInCall.class).forEach(n -> {
      dispatch(n, n.builtIn());
    });
  }


  @Override
  public void handleADDS(BuiltInCall input) {
    new ArithmeticInliner.AddS(input).inline();
  }

  @Override
  public void handleADDC(BuiltInCall input) {
    new ArithmeticInliner.AddC(input).inline();
  }

  @Override
  public void handleSSATADDS(BuiltInCall input) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void handleUSATADDS(BuiltInCall input) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void handleSSATADDC(BuiltInCall input) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void handleUSATADDC(BuiltInCall input) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void handleSUBSC(BuiltInCall input) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void handleSUBSB(BuiltInCall input) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void handleSUBC(BuiltInCall input) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void handleSUBB(BuiltInCall input) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void handleSSATSUBS(BuiltInCall input) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void handleUSATSUBS(BuiltInCall input) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void handleSSATSUBC(BuiltInCall input) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void handleUSATSUBC(BuiltInCall input) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void handleSSATSUBB(BuiltInCall input) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void handleUSATSUBB(BuiltInCall input) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void handleMULS(BuiltInCall input) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void handleSMULLS(BuiltInCall input) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void handleUMULLS(BuiltInCall input) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void handleSUMULLS(BuiltInCall input) {
    throw new UnsupportedOperationException("Not supported yet.");

  }

  @Override
  public void handleSMODS(BuiltInCall input) {
    throw new UnsupportedOperationException("Not supported yet.");

  }

  @Override
  public void handleUMODS(BuiltInCall input) {
    throw new UnsupportedOperationException("Not supported yet.");

  }

  @Override
  public void handleSDIVS(BuiltInCall input) {
    throw new UnsupportedOperationException("Not supported yet.");

  }

  @Override
  public void handleUDIVS(BuiltInCall input) {
    throw new UnsupportedOperationException("Not supported yet.");

  }

  @Override
  public void handleANDS(BuiltInCall input) {
    throw new UnsupportedOperationException("Not supported yet.");

  }

  @Override
  public void handleXORS(BuiltInCall input) {
    throw new UnsupportedOperationException("Not supported yet.");

  }

  @Override
  public void handleORS(BuiltInCall input) {
    throw new UnsupportedOperationException("Not supported yet.");

  }

  @Override
  public void handleLSLS(BuiltInCall input) {
    throw new UnsupportedOperationException("Not supported yet.");

  }

  @Override
  public void handleLSLC(BuiltInCall input) {
    throw new UnsupportedOperationException("Not supported yet.");

  }

  @Override
  public void handleASRS(BuiltInCall input) {
    throw new UnsupportedOperationException("Not supported yet.");

  }

  @Override
  public void handleLSRS(BuiltInCall input) {
    throw new UnsupportedOperationException("Not supported yet.");

  }

  @Override
  public void handleASRC(BuiltInCall input) {
    throw new UnsupportedOperationException("Not supported yet.");

  }

  @Override
  public void handleLSRC(BuiltInCall input) {
    throw new UnsupportedOperationException("Not supported yet.");

  }

  @Override
  public void handleROLS(BuiltInCall input) {
    throw new UnsupportedOperationException("Not supported yet.");

  }

  @Override
  public void handleROLC(BuiltInCall input) {
    throw new UnsupportedOperationException("Not supported yet.");

  }

  @Override
  public void handleRORS(BuiltInCall input) {
    throw new UnsupportedOperationException("Not supported yet.");

  }

  @Override
  public void handleRORC(BuiltInCall input) {
    throw new UnsupportedOperationException("Not supported yet.");

  }
}
