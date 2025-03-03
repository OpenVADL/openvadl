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

package vadl.lcb.passes.llvmLowering.domain.selectionDag;

import java.util.List;
import java.util.Set;
import vadl.lcb.passes.llvmLowering.LlvmNodeLowerable;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenMachineInstructionVisitor;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenNodeVisitor;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.ViamError;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * LLVM Node for logical comparison.
 */
public class LlvmSetccSD extends BuiltInCall implements LlvmNodeLowerable {
  public static Set<BuiltInTable.BuiltIn> supported = Set.of(
      BuiltInTable.EQU,
      BuiltInTable.NEQ,
      BuiltInTable.SGTH,
      BuiltInTable.UGTH,
      BuiltInTable.SLTH,
      BuiltInTable.ULTH,
      BuiltInTable.SLEQ,
      BuiltInTable.ULEQ,
      BuiltInTable.SGEQ,
      BuiltInTable.UGEQ
  );

  private LlvmCondCode llvmCondCode;

  /**
   * Constructor for LlvmSetccSD.
   */
  public LlvmSetccSD(BuiltInTable.BuiltIn built,
                     NodeList<ExpressionNode> args,
                     Type type) {
    super(built, args, type);
    this.builtIn = built;
    var condCode = LlvmCondCode.from(builtIn);
    if (condCode != null) {
      llvmCondCode = condCode;
    } else {
      throw new ViamError("not supported cond code");
    }
  }

  @Override
  public void setBuiltIn(BuiltInTable.BuiltIn builtIn) {
    this.builtIn = builtIn;
    var condCode = LlvmCondCode.from(builtIn);
    if (condCode != null) {
      llvmCondCode = condCode;
    } else {
      throw new ViamError("not supported cond code");
    }
  }

  @Override
  public String lower() {
    return "setcc";
  }

  public LlvmCondCode llvmCondCode() {
    return llvmCondCode;
  }

  /**
   * Gets the {@link BuiltInTable.BuiltIn}.
   */
  @Override
  public BuiltInTable.BuiltIn builtIn() {
    return this.builtIn;
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    if (visitor instanceof TableGenMachineInstructionVisitor v) {
      v.visit(this);
    } else if (visitor instanceof TableGenNodeVisitor v) {
      v.visit(this);
    } else {
      visitor.visit(this);
    }
  }

  @Override
  public ExpressionNode copy() {
    return new LlvmSetccSD(builtIn,
        new NodeList<>(args.stream().map(x -> (ExpressionNode) x.copy()).toList()),
        type());
  }

  @Override
  public Node shallowCopy() {
    return new LlvmSetccSD(builtIn, args, type());
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
  }
}
