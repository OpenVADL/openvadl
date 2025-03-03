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

package vadl.ast;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SyntaxTypeTest {
  @Test
  void coreTypeEqualityTest() {
    Assertions.assertEquals(BasicSyntaxType.STATS, BasicSyntaxType.STATS);
  }

  @Test
  void coreTypeSubtypeItselfTest() {
    Assertions.assertTrue(BasicSyntaxType.STATS.isSubTypeOf(BasicSyntaxType.STATS));
  }

  @Test
  void coreTypeSubtypeDirectParentTest() {
    Assertions.assertTrue(BasicSyntaxType.STAT.isSubTypeOf(BasicSyntaxType.STATS));
  }

  @Test
  void coreTypeSubtypeGrandparentsTest() {
    Assertions.assertTrue(BasicSyntaxType.BIN.isSubTypeOf(BasicSyntaxType.VAL));
    Assertions.assertTrue(BasicSyntaxType.BIN.isSubTypeOf(BasicSyntaxType.LIT));
    Assertions.assertTrue(BasicSyntaxType.BIN.isSubTypeOf(BasicSyntaxType.EX));
  }

  @Test
  void recordTypeParentTest() {
    var recordA = new RecordType("A", List.of(
        new RecordType.Entry("hello", BasicSyntaxType.ID),
        new RecordType.Entry("world", BasicSyntaxType.STAT)
    ));
    var recordB = new RecordType("B", List.of(
        new RecordType.Entry("names", BasicSyntaxType.EX),
        new RecordType.Entry("dontmatter", BasicSyntaxType.STATS)
    ));

    Assertions.assertTrue(recordA.isSubTypeOf(recordA), "A <: A");
    Assertions.assertTrue(recordB.isSubTypeOf(recordB), "B <: B");
    Assertions.assertTrue(recordA.isSubTypeOf(recordB), "A <: B");
    Assertions.assertFalse(recordB.isSubTypeOf(recordA), "B !<: A");
  }
}
