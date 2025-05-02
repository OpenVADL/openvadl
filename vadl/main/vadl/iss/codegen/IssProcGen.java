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

package vadl.iss.codegen;

import static vadl.error.DiagUtils.throwNotAllowed;

import vadl.cppCodeGen.context.CGenContext;
import vadl.cppCodeGen.context.CNodeContext;
import vadl.cppCodeGen.mixins.CDefaultMixins;
import vadl.cppCodeGen.mixins.CInvalidMixins;
import vadl.javaannotations.DispatchFor;
import vadl.javaannotations.Handler;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.graph.dependency.AsmBuiltInCall;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.passes.sideEffectScheduling.nodes.InstrExitNode;

@DispatchFor(
    value = Node.class,
    context = CNodeContext.class,
    include = "vadl.viam"
)
public abstract class IssProcGen implements CDefaultMixins.All,
    CInvalidMixins.ResourceReads, CInvalidMixins.SideEffect, CInvalidMixins.HardwareRelated {

  private final CNodeContext ctx;
  private final StringBuilder builder = new StringBuilder();

  public IssProcGen() {
    this.ctx = new CNodeContext(
        builder::append,
        (ctx, node)
            -> IssProcGenDispatcher.dispatch(this, ctx, node)
    );
  }

  protected CNodeContext ctx() {
    return ctx;
  }

  protected StringBuilder builder() {
    return builder;
  }

  @Handler
  void handle(CGenContext<Node> ctx, InstrExitNode.PcChange toHandle) {
    handle(ctx, (InstrExitNode) toHandle);
  }

  @Handler
  void handle(CGenContext<Node> ctx, FieldRefNode toHandle) {
    throwNotAllowed(toHandle, "Field references");
  }

  @Handler
  void handle(CGenContext<Node> ctx, FieldAccessRefNode toHandle) {
    throwNotAllowed(toHandle, "Field accesses");
  }

  @Handler
  void handle(CGenContext<Node> ctx, InstrCallNode toHandle) {
    throwNotAllowed(toHandle, "Instruction calls");
  }

  @Handler
  void handle(CGenContext<Node> ctx, AsmBuiltInCall toHandle) {
    throwNotAllowed(toHandle, "Assembler built-in calls");
  }

}
