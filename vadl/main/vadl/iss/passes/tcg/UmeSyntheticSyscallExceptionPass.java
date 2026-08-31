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
import java.util.function.BiConsumer;
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
 * This pass injects a synthetic exception raise into the {@code syscall instruction}.
 * The user does not have to define or reference an exception, this mechanism
 * works internally. The user only declares which instruction triggers a syscall.
 */
public class UmeSyntheticSyscallExceptionPass extends AbstractIssPass {

  private static final ExceptionDef UME_SYSCALL_EXC = new ExceptionDef(
      new Identifier(List.of("ume_syscall"), SourceLocation.INVALID_SOURCE_LOCATION),
      new Parameter[]{},
      createDummySyscallGraph(),
      ExceptionDef.Kind.ANONYMOUS
  );

  public UmeSyntheticSyscallExceptionPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("UME Synthetic Syscall Exception");
  }

  BiConsumer<Instruction, Instruction> injectSyscallRaise = (instr, syscallInstr) -> {

    if (!instr.equals(syscallInstr)) {
      return;
    }

    var graph = instr.behavior();

    var instrEnd = graph.getNodes(InstrEndNode.class).findFirst().orElseThrow();

    instrEnd.addBefore(new TcgGenException(UME_SYSCALL_EXC, new NodeList<>()));
  };

  List<BiConsumer<Instruction, Instruction>> instrAddOns = List.of(
      injectSyscallRaise
  );

  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam)
      throws IOException {

    var ume = viam.userModeEmulation();
    if (ume.isEmpty()) {
      return null;
    }

    var syscallInstr = ume.get().syscallInstr();

    var isa = viam.isa().orElseThrow();
    var excInfo = isa.expectExtension(ExceptionInfo.class);
    excInfo.addException(UME_SYSCALL_EXC);

    normalTcgInstrs(viam).forEach(i -> instrAddOns.forEach(f -> f.accept(i, syscallInstr)));
    return null;
  }

  private static Graph createDummySyscallGraph() {
    var g = new Graph("synthetic-ume-syscall");

    var end = g.addWithInputs(new ProcEndNode(new NodeList<>()));

    var start = new StartNode(end);
    g.addWithInputs(start);

    return g;
  }
}
