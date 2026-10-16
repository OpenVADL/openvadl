// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.vdt.impl.irregular.vliw.utils.assertions;

import org.junit.jupiter.api.Assertions;
import vadl.vdt.impl.vliw.model.PointedExpression;

public class PointedExpressionAssert {
  PointedExpression actual;

  public PointedExpressionAssert(PointedExpression actual) {
    this.actual = actual;
  }

  public static PointedExpressionAssert assertThat(PointedExpression actual) {
    return new PointedExpressionAssert(actual);
  }

  public PointedExpressionAssert is(String expected) {
    Assertions.assertEquals(expected, actual.toString(true));
    return this;
  }

  public PointedExpressionAssert hasItem(String expected) {
    Assertions.assertEquals(expected, actual.item().toString(true));
    return this;
  }

  public PointedExpressionAssert hasTrailing(boolean expected) {
    Assertions.assertEquals(expected, actual.trailing());
    return this;
  }

  public PointedExpressionAssert isTrailing() {
    return hasTrailing(true);
  }

  public PointedExpressionAssert notTrailing() {
    return hasTrailing(false);
  }
}
