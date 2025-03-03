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

package vadl.viam.graph.control;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import vadl.javaannotations.viam.Successor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;

/**
 * The DirectionalNode is a type of control flow node with exactly one
 * successor node.
 */
public abstract class DirectionalNode extends ControlNode {

  @Successor
  // even though it is nullable, the next node is not optional!
  private @Nullable ControlNode next;

  protected DirectionalNode() {
  }

  /**
   * The variant if it is possible to directly set the next node construction.
   */
  protected DirectionalNode(@Nonnull ControlNode next) {
    this.next = next;
  }

  @Override
  public void verifyState() {
    super.verifyState();
    ensure(next != null, "next node must always be set directly after construction of this");
  }

  /**
   * Sets the successor property of this node.
   *
   * <p>It is important that this is done right after creation.
   * The successor field should never be null.
   *
   * @param next the successor of this node
   */
  public void setNext(@Nullable ControlNode next) {
    this.updatePredecessorOf(this.next, next);
    this.next = next;
  }

  /**
   * Inserts a direction node between this and the next node.
   * If the new node is not yet active, it will be added to the graph.
   *
   * @param newNode node to be inserted.
   */
  public <T extends DirectionalNode> T addAfter(@Nonnull T newNode) {
    ensure(isActive() && graph() != null, "Node is not active");
    if (!newNode.isActive()) {
      newNode = graph().addWithInputs(newNode);
    }
    var next = this.next();
    // remove predecessor of the next node
    this.setNext(null);
    newNode.setNext(next);
    this.setNext(newNode);
    return newNode;
  }


  /**
   * Replaces this node with its successor, and then safely deletes this node.
   *
   * <p>The method ensures that the node to be deleted has a predecessor, then it updates
   * the predecessor's successor to bypass this node. Finally, it safely deletes this node
   * from the graph to maintain consistency.
   */
  public void replaceByNothingAndDelete() {
    ensure(this.predecessor() != null, "Can only remove this node if the predecessor is set!");
    // set successor to successor of predecessor
    var successor = this.next();
    replaceSuccessor(successor, null);
    predecessor().replaceSuccessor(this, successor);
    safeDelete();
  }

  /**
   * This will replace this node by the given node and also links the new node
   * to the next one.
   * <pre>{@code
   * Before:
   * X --->     This    ---> Y
   * After:
   * X ---> Replacement ---> Y
   * }</pre>
   * After linking, this node is NOT deleted.
   *
   * @param replacement The replacement of the current node
   */
  public void replaceAndLink(DirectionalNode replacement) {
    replace(replacement);
    var next = this.next();
    setNext(null);
    replacement.setNext(next);
  }

  public ControlNode next() {
    ensure(next != null, "next node is null but must be set!");
    return next;
  }

  @Override
  protected void collectSuccessors(List<Node> collection) {
    super.collectSuccessors(collection);
    if (this.next != null) {
      collection.add(next);
    }
  }

  @Override
  public void applyOnSuccessorsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnSuccessorsUnsafe(visitor);
    next = visitor.applyNullable(this, next, ControlNode.class);
  }
}
