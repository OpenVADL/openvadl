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

package vadl.viam.graph.visualize;

import vadl.viam.graph.Graph;

/**
 * The GraphVisualizer interface defines methods for visualizing a graph.
 *
 * @param <R> the type of the visualization result
 * @param <G> the type of the graph to visualize
 */
public interface GraphVisualizer<R, G extends Graph> {

  /**
   * Loads a graph into the GraphVisualizer.
   *
   * @param graph the graph to load into the GraphVisualizer
   * @return the GraphVisualizer instance
   */
  GraphVisualizer<R, G> load(G graph);

  /**
   * Generates a visualization of the graph.
   *
   * @return the visualization result
   */
  R visualize();
}
