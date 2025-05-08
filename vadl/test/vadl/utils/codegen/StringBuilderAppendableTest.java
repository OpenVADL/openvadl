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

package vadl.utils.codegen;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vadl.utils.codegen.StringBuilderAppendable;

public class StringBuilderAppendableTest {

  @Test
  void testAppendGeneric() {

    /* GIVEN */
    final var a = new StringBuilderAppendable();

    /* WHEN */
    a.append((short) 1).newLine();
    a.append((byte) 2).newLine();
    a.append(3).newLine();
    a.append(4L).newLine();
    a.append(1.2f).newLine();
    a.append(1.3d).newLine();
    a.append('a').newLine();
    a.append(true).newLine();
    a.append((Object) "b");

    /* THEN */
    var expected = """
        1
        2
        3
        4
        1.2
        1.3
        a
        true
        b""";
    Assertions.assertEquals(expected, a.toString());
  }

  @Test
  void testAppendNewLineChar_indent() {

    /* GIVEN */
    final var a = new StringBuilderAppendable();

    /* WHEN */
    a.indent()
        .append("foo")
        .append('\n')
        .append("bar");

    /* THEN */
    Assertions.assertEquals("  foo\n  bar", a.toString());
  }

  @Test
  void testAppendCharSeq_splitNewLineAndIndent() {

    /* GIVEN */
    final var a = new StringBuilderAppendable();

    /* WHEN */
    a.indent().append("foo\nbar");

    /* THEN */
    Assertions.assertEquals("  foo\n  bar", a.toString());
  }

  @Test
  void testAppendCharSeq_indent() {

    /* GIVEN */
    final var a = new StringBuilderAppendable();

    /* WHEN */
    a.indent()
        .append("foo\n")
        .indent()
        .append("bar\n")
        .unindent()
        .append("baz");


    /* THEN */
    Assertions.assertEquals("  foo\n    bar\n  baz", a.toString());
  }

  @Test
  void testAppendCharSeq_unindent() {

    /* GIVEN */
    final var a = new StringBuilderAppendable();

    /* WHEN */
    a.indent()
        .append("foo\n")
        .indent()
        .append("bar\n")
        .unindent()
        .indent().unindent()
        .append("baz");


    /* THEN */
    Assertions.assertEquals("  foo\n    bar\n  baz", a.toString());
  }

  @Test
  void testAppendCodePoint() {

    /* GIVEN */
    final var a = new StringBuilderAppendable();

    /* WHEN */
    a.indent()
        .append("foo 🌻\nbar 🌱")
        .newLine()
        .unindent()
        .append("baz");


    /* THEN */
    Assertions.assertEquals("  foo 🌻\n  bar 🌱\nbaz", a.toString());
  }

  @Test
  void testSplitByNewLine() {

    /* GIVEN */
    final var a = new StringBuilderAppendable();

    /* WHEN */
    a.append("foo")
        .newLine()
        .indent()
        .append("1\n2")
        .newLine()
        .append("3\r4")
        .newLine()
        .append("5\f6")
        .newLine()
        .append("7\u00858")
        .newLine()
        .append("9\u202810")
        .newLine()
        .append("11\u202912");


    /* THEN */
    var expected = """
        foo
          1
          2
          3
          4
          5
          6
          7
          8
          9
          10
          11
          12""";
    Assertions.assertEquals(expected, a.toString());
  }
}
