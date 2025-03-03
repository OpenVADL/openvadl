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

package vadl.vdt.target.common.dto;

/**
 * Holds statistics about a decision tree.
 */
public class DecisionTreeStatistics {

  private int numberOfNodes;
  private int numberOfLeafNodes;

  private int maxDepth;
  private int minDepth;
  private double avgDepth;

  private int maxInstructionWidth;

  public int getNumberOfNodes() {
    return numberOfNodes;
  }

  public void setNumberOfNodes(int numberOfNodes) {
    this.numberOfNodes = numberOfNodes;
  }

  public int getNumberOfLeafNodes() {
    return numberOfLeafNodes;
  }

  public void setNumberOfLeafNodes(int numberOfLeafNodes) {
    this.numberOfLeafNodes = numberOfLeafNodes;
  }

  public int getMaxDepth() {
    return maxDepth;
  }

  public void setMaxDepth(int maxDepth) {
    this.maxDepth = maxDepth;
  }

  public int getMinDepth() {
    return minDepth;
  }

  public void setMinDepth(int minDepth) {
    this.minDepth = minDepth;
  }

  public double getAvgDepth() {
    return avgDepth;
  }

  public void setAvgDepth(double avgDepth) {
    this.avgDepth = avgDepth;
  }

  public int getMaxInstructionWidth() {
    return maxInstructionWidth;
  }

  public void setMaxInstructionWidth(int maxInstructionWidth) {
    this.maxInstructionWidth = maxInstructionWidth;
  }

  @Override
  public String toString() {
    return "{\n"
        + "  numberOfNodes: " + numberOfNodes + ",\n"
        + "  numberOfLeafNodes: " + numberOfLeafNodes + ",\n"
        + "  maxDepth: " + maxDepth + ",\n"
        + "  minDepth: " + minDepth + ",\n"
        + "  avgDepth: " + avgDepth + "\n"
        + "  maxInsnWidth: " + maxInstructionWidth + "\n"
        + "}";
  }
}
