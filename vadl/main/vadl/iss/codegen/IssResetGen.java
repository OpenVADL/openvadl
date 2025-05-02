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

import static vadl.utils.GraphUtils.getSingleNode;

import vadl.cppCodeGen.context.CGenContext;
import vadl.viam.Procedure;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;
import vadl.viam.passes.sideEffectScheduling.nodes.InstrExitNode;

public class IssResetGen extends IssProcGen {

  private final Procedure reset;

  public IssResetGen(Procedure reset) {
    this.reset = reset;
  }

  @Override
  void handle(CGenContext<Node> ctx, InstrExitNode.PcChange toHandle) {
    ctx.gen(toHandle.cause())
        .ln(";").gen(toHandle.next());
  }

  @Override
  public void handle(CGenContext<Node> ctx, WriteRegTensorNode toHandle) {
    var reg = toHandle.regTensor();
    ctx().wr("set_" + reg.simpleName().toLowerCase() + "(");
    for (var i : toHandle.indices()) {
      ctx().gen(i).wr(",");
    }
    ctx().gen(toHandle.value()).wr(")");
  }

  public String fetch() {
    var start = getSingleNode(reset.behavior(), StartNode.class);
    var current = start.next();
    ctx()
        .spacedIn()
        .gen(current);
    return builder().toString();
  }

}
