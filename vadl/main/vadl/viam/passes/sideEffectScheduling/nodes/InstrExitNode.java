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

package vadl.viam.passes.sideEffectScheduling.nodes;

import java.util.List;
import vadl.javaannotations.viam.Input;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.DirectionalNode;
import vadl.viam.graph.dependency.ProcCallNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.WriteResourceNode;

/**
 * Represents the exit point of an instruction in the side-effect scheduling pass.
 * This node is a {@link DirectionalNode} that signifies the completion of an instruction,
 * specifically handling the write to the program counter (PC) or raise of an exception.
 *
 * @see PcChange
 * @see Raise
 * @see vadl.viam.passes.sideEffectScheduling.SideEffectSchedulingPass
 * @see vadl.iss.passes.IssPcAccessConversionPass
 * @see vadl.iss.passes.tcgLowering.TcgOpLoweringPass
 */
public abstract sealed class InstrExitNode extends DirectionalNode permits InstrExitNode.PcChange,
    InstrExitNode.Raise {

  /**
   * An instruction exit that is caused by e PC change.
   * If the PC is modified in an instruction, it means a jump from a QEMU perspective.
   *
   * @see InstrExitNode
   */
  public static final class PcChange extends InstrExitNode {

    /**
     * The side effect operation causing the instruction exit.
     * This is either a {@link WriteResourceNode} to the PC or
     * a {@link vadl.viam.graph.dependency.ProcCallNode} representing a raise.
     */
    @Input
    private WriteResourceNode cause;

    /**
     * Constructs an {@code InstrExitNode} with the specified PC write operation.
     *
     * @param cause The {@link WriteResourceNode} representing the write to the program counter.
     */
    public PcChange(WriteResourceNode cause) {
      this.cause = cause;
    }

    @Override
    public WriteResourceNode cause() {
      return cause;
    }

    @Override
    public Node copy() {
      return new InstrExitNode.PcChange(cause.copy(WriteResourceNode.class));
    }

    @Override
    public Node shallowCopy() {
      return new InstrExitNode.PcChange(cause);
    }

    @Override
    public <T extends GraphNodeVisitor> void accept(T visitor) {
      // not used
    }

    @Override
    protected void collectInputs(List<Node> collection) {
      super.collectInputs(collection);
      collection.add(cause);
    }

    @Override
    protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
      super.applyOnInputsUnsafe(visitor);
      cause = visitor.apply(this, cause, WriteResourceNode.class);
    }
  }

  /**
   * An instruction exit that is caused by an exception raise.
   *
   * @see InstrExitNode
   */
  public static final class Raise extends InstrExitNode {

    /**
     * The side effect operation causing the instruction exit.
     * This is either a {@link WriteResourceNode} to the PC or
     * a {@link vadl.viam.graph.dependency.ProcCallNode} representing a raise.
     */
    @Input
    private ProcCallNode cause;


    /**
     * Constructs an {@code InstrExitNode} with the specified PC write operation.
     *
     * @param cause The {@link WriteResourceNode} representing the write to the program counter.
     */
    public Raise(ProcCallNode cause) {
      this.cause = cause;
    }

    @Override
    public ProcCallNode cause() {
      return cause;
    }

    @Override
    public Node copy() {
      return new InstrExitNode.Raise(cause.copy(ProcCallNode.class));
    }

    @Override
    public Node shallowCopy() {
      return new InstrExitNode.Raise(cause);
    }

    @Override
    public <T extends GraphNodeVisitor> void accept(T visitor) {
      // not used
    }

    @Override
    protected void collectInputs(List<Node> collection) {
      super.collectInputs(collection);
      collection.add(cause);
    }

    @Override
    protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
      super.applyOnInputsUnsafe(visitor);
      cause = visitor.apply(this, cause, ProcCallNode.class);
    }
  }


  /**
   * Returns the {@link SideEffectNode} that causes the instruction exit.
   *
   * @return The PC write operation.
   */
  public abstract SideEffectNode cause();
}