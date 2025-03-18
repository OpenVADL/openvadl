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

package vadl.iss.passes;

import static vadl.utils.GraphUtils.getSingleNode;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.tcgLowering.nodes.TcgGenException;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.graph.control.InstrEndNode;

/**
 * This pass manipulates the VIAM with hardcoded elements.
 * E.g. it adds an exception generation to {@code ECALL} instruction because
 * this is not yet supported in the VADL specification.
 */
public class IssHardcodedTcgAddOnPass extends AbstractIssPass {

  public IssHardcodedTcgAddOnPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("ISS Hardcoded TCG Add-Ons");
  }

  List<Consumer<Instruction>> instrAddOns = List.of(
      this::ecallAddExceptionRaise
  );

  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam)
      throws IOException {

    viam.isa().ifPresent(isa ->
        isa.ownInstructions()
            .forEach(i ->
                instrAddOns.forEach(f -> f.accept(i))));

    return null;
  }


  /*
   * Add a tcg helper call to raise_exception in ecall. We do this as the current
   * specification does not yet implement it, however, it is necessary for tests.
   */
  private void ecallAddExceptionRaise(Instruction instr) {
    if (!instr.simpleName().equals("ECALL")) {
      return;
    }

    var instrEnd = getSingleNode(instr.behavior(), InstrEndNode.class);
    instrEnd.addBefore(new TcgGenException(0xb));
  }

}
