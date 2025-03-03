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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static vadl.TestUtils.createParameter;

import org.junit.jupiter.api.Test;
import vadl.AbstractTest;
import vadl.types.DataType;
import vadl.viam.graph.dependency.FuncParamNode;

class FuncParamMatcherTest extends AbstractTest {
  @Test
  void matches_shouldReturnTrue_whenTypeMatches() {
    var matcher = new FuncParamMatcher(DataType.unsignedInt(32));
    var node = new FuncParamNode(createParameter("parameterValue", DataType.unsignedInt(32)));

    // When
    var result = matcher.matches(node);

    // Then
    assertThat(result).isTrue();
  }

  @Test
  void matches_shouldReturnFalse_whenTypeMismatches() {
    var matcher = new FuncParamMatcher(DataType.bool());
    var node = new FuncParamNode(createParameter("parameterValue", DataType.unsignedInt(32)));

    // When
    var result = matcher.matches(node);

    // Then
    assertThat(result).isFalse();
  }

}