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

package vadl.viam.graph;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A iterator that is used to iterate over nodes in a graph.
 */
public interface NodeIter<T> extends Iterator<T> {

  /**
   * This iterator iterates over a snapshot of the graph.
   * This means it will only iterate over nodes that are in the graph at the time of
   * creating the iterator.
   * So if nodes are added during iteration, those nodes are not getting iterated.
   * However, if nodes are getting deleted during the iteration and were not yet iterated,
   * they will never be iterated.
   */
  class SnapshotIter implements NodeIter<Node> {

    private final int sizeAtCreation;
    protected int currentIndex;
    protected final Graph graph;

    public SnapshotIter(Graph graph) {
      this.graph = graph;
      sizeAtCreation = graph.nodes.size();
    }

    @Override
    public boolean hasNext() {
      while (currentIndex < sizeAtCreation && graph.nodes.get(currentIndex) == null) {
        currentIndex++;  // Skip null entries
      }
      return currentIndex < sizeAtCreation;
    }

    @Override
    public Node next() {
      if (currentIndex >= sizeAtCreation) {
        throw new NoSuchElementException("No more nodes available");
      }
      Node node = graph.nodes.get(currentIndex);
      currentIndex++;  // Move to the next index for future calls
      return node;
    }
  }
}
