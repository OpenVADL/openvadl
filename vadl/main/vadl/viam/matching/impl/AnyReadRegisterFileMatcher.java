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

package vadl.viam.matching.impl;

import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ReadArtificialResNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.matching.Matcher;

/**
 * Matches any {@link ReadRegTensorNode} or {@link ReadArtificialResNode} that is a register file
 * (two dimensions).
 */
public class AnyReadRegisterFileMatcher implements Matcher {

  @Override
  public boolean matches(Node node) {
    var isReadRegTensorNode = node instanceof ReadRegTensorNode readRegTensorNode
        && readRegTensorNode.regTensor().isRegisterFile();
    var isReadArtificialResNode = node instanceof ReadArtificialResNode readArtificialResNode
        && readArtificialResNode.hasRegisterFile();
    return isReadRegTensorNode || isReadArtificialResNode;
  }
}
