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

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;


/**
 * The GraphMatchers class is a collection of matcher methods for nodes in a graph.
 */
public class GraphMatchers {

  /**
   * Returns a Matcher object that matches nodes that are active in the given graph.
   *
   * @param graph the graph to check for node activity
   * @return a Matcher object that matches active nodes in the given graph
   */
  public static Matcher<Node> activeIn(Graph graph) {
    return new TypeSafeMatcher<>() {
      @Override
      public void describeTo(Description description) {
        description.appendText("active in graph ");
      }

      @Override
      protected boolean matchesSafely(Node node) {
        return node.isActiveIn(graph);
      }
    };
  }
}
