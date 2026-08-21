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

package vadl.iss.passes.tcg;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.AbstractIssPass;
import vadl.iss.passes.extensions.ExceptionInfo;
import vadl.iss.passes.tcg.lowering.nodes.TcgGenException;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.SourceLocation;
import vadl.viam.ExceptionDef;
import vadl.viam.Identifier;
import vadl.viam.Instruction;
import vadl.viam.Parameter;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.InstrEndNode;
import vadl.viam.graph.control.ProcEndNode;
import vadl.viam.graph.control.StartNode;

/**
 * This pass manipulates the VIAM with hardcoded elements.
 * E.g. it adds an exception generation to {@code ECALL} instruction because
 * this is not yet supported in the VADL specification.
 */
public class IssHardcodedTcgAddOnPass extends AbstractIssPass {

  private static Graph createDummySyscallGraph() {
    var g = new Graph("synthetic-ume-syscall");
    g.setSourceLocation(SourceLocation.INVALID_SOURCE_LOCATION);

    var end = g.addWithInputs(new ProcEndNode(new NodeList<>()));
    end.setSourceLocation(SourceLocation.INVALID_SOURCE_LOCATION);

    var start = new StartNode(end);
    start.setSourceLocation(SourceLocation.INVALID_SOURCE_LOCATION);
    g.addWithInputs(start);

    return g;
  }

  private static final ExceptionDef UME_SYSCALL_EXC = new ExceptionDef(
      new Identifier(List.of("ume_syscall"), SourceLocation.INVALID_SOURCE_LOCATION),
      new Parameter[]{},
      createDummySyscallGraph(),
      ExceptionDef.Kind.ANONYMOUS
  );

  public IssHardcodedTcgAddOnPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("ISS Hardcoded TCG Add-Ons");
  }

  Consumer<Instruction> injectSyscallRaise = instr -> {

    if (!"ECALL".equalsIgnoreCase(instr.simpleName())) {
      return;
    }

    var graph = instr.behavior();

    var instrEnd = graph.getNodes(InstrEndNode.class).findFirst().orElseThrow();

    instrEnd.addBefore(new TcgGenException(UME_SYSCALL_EXC, new NodeList<>()));
  };

  List<Consumer<Instruction>> instrAddOns = List.of(
      injectSyscallRaise
  );

  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam)
      throws IOException {

    var isa = viam.isa().orElseThrow();
    var excInfo = isa.expectExtension(ExceptionInfo.class);
    excInfo.addException(UME_SYSCALL_EXC);

    normalTcgInstrs(viam).forEach(i -> instrAddOns.forEach(f -> f.accept(i)));

    return null;
  }


}
