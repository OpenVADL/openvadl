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

import org.junit.jupiter.api.Test;
import vadl.AbstractTest;
import vadl.types.DataType;
import vadl.viam.Constant;
import vadl.viam.graph.dependency.ConstantNode;

class AnyConstantValueMatcherTest extends AbstractTest {

  @Test
  void matches_shouldReturnTrue_whenConstantValue() {
    var matcher = new AnyConstantValueMatcher();
    var node = new ConstantNode(Constant.Value.of(0, DataType.unsignedInt(32)));

    // When
    var result = matcher.matches(node);

    // Then
    assertThat(result).isTrue();
  }

  @Test
  void matches_shouldReturnFalse_whenConstantBitSlice() {
    var matcher = new AnyConstantValueMatcher();
    var node = new ConstantNode(
        new Constant.BitSlice(new Constant.BitSlice.Part[] {
            new Constant.BitSlice.Part(0, 0)
        }));

    // When
    var result = matcher.matches(node);

    // Then
    assertThat(result).isFalse();
  }


  @Test
  void matches_shouldReturnFalse_whenConstantString() {
    var matcher = new AnyConstantValueMatcher();
    var node = new ConstantNode(new Constant.Str(""));

    // When
    var result = matcher.matches(node);

    // Then
    assertThat(result).isFalse();
  }
}