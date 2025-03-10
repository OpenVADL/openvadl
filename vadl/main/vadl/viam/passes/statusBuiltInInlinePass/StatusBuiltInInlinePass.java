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
import vadl.error.Diagnostic;
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

  private void throwNotImplemented(BuiltInCall input) {
    throw Diagnostic.error("Built-In Lowering Not Implemented", input)
        .description("OpenVADL does not yet implement the inlining of the %s built-in.",
            input.builtIn().name())
        .build();
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
    throwNotImplemented(input);
  }

  @Override
  public void handleUSATADDS(BuiltInCall input) {
    throwNotImplemented(input);
  }

  @Override
  public void handleSSATADDC(BuiltInCall input) {
    throwNotImplemented(input);
  }

  @Override
  public void handleUSATADDC(BuiltInCall input) {
    throwNotImplemented(input);
  }

  @Override
  public void handleSUBSC(BuiltInCall input) {
    new ArithmeticInliner.SubSC(input).inline();
  }

  @Override
  public void handleSUBSB(BuiltInCall input) {
    new ArithmeticInliner.SubSB(input).inline();
  }

  @Override
  public void handleSUBC(BuiltInCall input) {
    new ArithmeticInliner.SubC(input).inline();
  }

  @Override
  public void handleSUBB(BuiltInCall input) {
    new ArithmeticInliner.SubB(input).inline();
  }

  @Override
  public void handleSSATSUBS(BuiltInCall input) {
    throwNotImplemented(input);
  }

  @Override
  public void handleUSATSUBS(BuiltInCall input) {
    throwNotImplemented(input);
  }

  @Override
  public void handleSSATSUBC(BuiltInCall input) {
    throwNotImplemented(input);
  }

  @Override
  public void handleUSATSUBC(BuiltInCall input) {
    throwNotImplemented(input);
  }

  @Override
  public void handleSSATSUBB(BuiltInCall input) {
    throwNotImplemented(input);
  }

  @Override
  public void handleUSATSUBB(BuiltInCall input) {
    throwNotImplemented(input);
  }

  @Override
  public void handleMULS(BuiltInCall input) {
    throwNotImplemented(input);
  }

  @Override
  public void handleSMULLS(BuiltInCall input) {
    throwNotImplemented(input);
  }

  @Override
  public void handleUMULLS(BuiltInCall input) {
    throwNotImplemented(input);
  }

  @Override
  public void handleSUMULLS(BuiltInCall input) {
    throwNotImplemented(input);

  }

  @Override
  public void handleSMODS(BuiltInCall input) {
    throwNotImplemented(input);

  }

  @Override
  public void handleUMODS(BuiltInCall input) {
    throwNotImplemented(input);

  }

  @Override
  public void handleSDIVS(BuiltInCall input) {
    throwNotImplemented(input);

  }

  @Override
  public void handleUDIVS(BuiltInCall input) {
    throwNotImplemented(input);

  }

  @Override
  public void handleANDS(BuiltInCall input) {
    throwNotImplemented(input);

  }

  @Override
  public void handleXORS(BuiltInCall input) {
    throwNotImplemented(input);

  }

  @Override
  public void handleORS(BuiltInCall input) {
    throwNotImplemented(input);

  }

  @Override
  public void handleLSLS(BuiltInCall input) {
    throwNotImplemented(input);

  }

  @Override
  public void handleLSLC(BuiltInCall input) {
    throwNotImplemented(input);

  }

  @Override
  public void handleASRS(BuiltInCall input) {
    throwNotImplemented(input);

  }

  @Override
  public void handleLSRS(BuiltInCall input) {
    throwNotImplemented(input);

  }

  @Override
  public void handleASRC(BuiltInCall input) {
    throwNotImplemented(input);

  }

  @Override
  public void handleLSRC(BuiltInCall input) {
    throwNotImplemented(input);

  }

  @Override
  public void handleROLS(BuiltInCall input) {
    throwNotImplemented(input);

  }

  @Override
  public void handleROLC(BuiltInCall input) {
    throwNotImplemented(input);

  }

  @Override
  public void handleRORS(BuiltInCall input) {
    throwNotImplemented(input);

  }

  @Override
  public void handleRORC(BuiltInCall input) {
    throwNotImplemented(input);
  }
}
