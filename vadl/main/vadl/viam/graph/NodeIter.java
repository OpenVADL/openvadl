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

package vadl.viam.graph;

import static java.util.Objects.requireNonNull;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * A iterator that is used to iterate over nodes in a graph.
 */
public interface NodeIter<T> extends Iterator<T> {

  /**
   * Base implementation for iterating over a graph snapshot while optionally filtering nodes.
   */
  abstract class AbstractSnapshotIter<T> implements NodeIter<T> {

    private final int sizeAtCreation;
    protected int currentIndex;
    protected final Graph graph;
    private @Nullable T nextNode;
    private boolean nextNodeReady;

    protected AbstractSnapshotIter(Graph graph) {
      this.graph = graph;
      this.sizeAtCreation = graph.nodes.size();
    }

    protected abstract @Nullable T tryConvert(Node node);

    @Override
    public boolean hasNext() {
      if (nextNodeReady) {
        return true;
      }

      while (currentIndex < sizeAtCreation) {
        Node node = graph.nodes.get(currentIndex);
        currentIndex++;

        if (node == null) {
          continue;
        }

        var converted = tryConvert(node);
        if (converted != null) {
          nextNode = converted;
          nextNodeReady = true;
          return true;
        }
      }

      return false;
    }

    @Override
    public T next() {
      if (!hasNext()) {
        throw new NoSuchElementException("No more nodes available");
      }

      nextNodeReady = false;
      var result = nextNode;
      nextNode = null;
      return requireNonNull(result);
    }
  }

  /**
   * This iterator iterates over a snapshot of the graph.
   * This means it will only iterate over nodes that are in the graph at the time of
   * creating the iterator.
   * So if nodes are added during iteration, those nodes are not getting iterated.
   * However, if nodes are getting deleted during the iteration and were not yet iterated,
   * they will never be iterated.
   */
  class SnapshotIter extends AbstractSnapshotIter<Node> {

    public SnapshotIter(Graph graph) {
      super(graph);
    }

    @Override
    protected Node tryConvert(Node node) {
      return node;
    }
  }

  /**
   * This iterator iterates over nodes of a specific type in a graph snapshot.
   */
  class TypedSnapshotIter<T> extends AbstractSnapshotIter<T> {

    private final Class<T> clazz;

    public TypedSnapshotIter(Graph graph, Class<T> clazz) {
      super(graph);
      this.clazz = clazz;
    }

    @Override
    protected @Nullable T tryConvert(Node node) {
      return clazz.isInstance(node) ? clazz.cast(node) : null;
    }
  }

  /**
   * This iterator iterates over nodes matching any of the provided types in a graph snapshot.
   */
  class MultiTypeSnapshotIter extends AbstractSnapshotIter<Node> {

    private final Set<Class<?>> classes;

    public MultiTypeSnapshotIter(Graph graph, Set<Class<?>> classes) {
      super(graph);
      this.classes = classes;
    }

    @Override
    protected @Nullable Node tryConvert(Node node) {
      for (var clazz : classes) {
        if (clazz.isInstance(node)) {
          return node;
        }
      }
      return null;
    }
  }
}
