// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.utils;

import static java.util.Objects.requireNonNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link SourceLocation} class. Especially the toSourceString() method.
 */
public class SourceLocationTest {

  private static Path miniVadlPath;

  @BeforeAll
  public static void setup() throws URISyntaxException {
    miniVadlPath =
        Paths.get(
            requireNonNull(SourceLocationTest.class.getResource("/testFiles/mini.vadl")).getPath());
  }


  @Test
  public void testToSourceString_singleLine() {
    SourceLocation location = new SourceLocation.DirectLocation(miniVadlPath, 12);
    String expected =
        "  constant MLen   = $ArchSize()           // MLen = 32 or 64 depending on ArchSize";
    assertEquals(expected, location.toSourceString(new DiskVirtualFileSystem()));
  }

  @Test
  public void testToSourceString_multipleLines() {
    SourceLocation location = new SourceLocation.DirectLocation(miniVadlPath, 14, 16);
    String expected = "  using Inst     = Bits<32>               // instruction word is 32 bit\n"
        + "  using Regs     = Bits<MLen>             // untyped register word type\n"
        + "  using Bits3    = Bits< 3>               // 3 bit type";
    assertEquals(expected, location.toSourceString(new DiskVirtualFileSystem()));
  }

  @Test
  public void testToSourceString_withColumn() {
    SourceLocation.Position start = new SourceLocation.Position(23, 10);
    SourceLocation.Position end = new SourceLocation.Position(23, 15);
    SourceLocation location = new SourceLocation.DirectLocation(miniVadlPath, start, end);
    String expected = "Rtype";
    assertEquals(expected, location.toSourceString(new DiskVirtualFileSystem()));
  }

  @Test
  public void testToSourceString_multipleLinesWithColumn() {
    SourceLocation.Position start = new SourceLocation.Position(33, 3);
    SourceLocation.Position end = new SourceLocation.Position(34, 61);
    SourceLocation location = new SourceLocation.DirectLocation(miniVadlPath, start, end);
    String expected =
        "instruction ADD : Rtype =               // 3 register operand instructions\n"
            + "      X(rd) := ((X(rs1) as Bits) + (X(rs2) as Bits)) as Regs";
    assertEquals(expected, location.toSourceString(new DiskVirtualFileSystem()));
  }

  @Test
  public void testToSourceString_whenBeginLineIsZero() {
    SourceLocation location = new SourceLocation.DirectLocation(miniVadlPath, 0);
    assert (location.toSourceString(new DiskVirtualFileSystem()).contains("Invalid source location"));
  }

  @Test
  public void testToUriString() {
    SourceLocation location = new SourceLocation.DirectLocation(miniVadlPath, new SourceLocation.Position(1, 5));
    assertThat(location.toUriString(), startsWith("file:/"));
    assertThat(location.toUriString(), endsWith("mini.vadl:1:5 .. 1:5"));
  }

  @Test
  public void testJoin_DirectLocations() {
    var resultStart = new SourceLocation.Position(1, 5);
    var resultEnd = new SourceLocation.Position(4, 2);

    var locationA = new SourceLocation.DirectLocation(miniVadlPath,
        resultStart,
        new SourceLocation.Position(2, 3)
    );
    var locationB = new SourceLocation.DirectLocation(miniVadlPath,
        new SourceLocation.Position(4, 1),
        resultEnd
    );

    var result1 = locationA.join(locationB);
    var result2 = locationB.join(locationA);

    assertEquals(
        new SourceLocation.DirectLocation(miniVadlPath, resultStart, resultEnd),
        result1
    );
    assertEquals(result1, result2, "join() should be commutative");
  }

  @Test
  public void testJoin_DirectAndExpandedLocation() {
    var resultStart = new SourceLocation.Position(2, 1);
    var resultEnd = new SourceLocation.Position(10, 3);

    var locationA = new SourceLocation.DirectLocation(miniVadlPath,
        resultStart,
        new SourceLocation.Position(2, 3)
    );
    var locationB = SourceLocation.of(miniVadlPath,
        // This primary location should be ignored
        new SourceLocation.Position(44, 33),
        new SourceLocation.Position(111, 222),

        List.of(new SourceLocation.DirectLocation(miniVadlPath,
            new SourceLocation.Position(4, 1), resultEnd)
        )
    );

    var result1 = locationA.join(locationB);
    var result2 = locationB.join(locationA);

    assertEquals(
        new SourceLocation.DirectLocation(miniVadlPath, resultStart, resultEnd),
        result1
    );
    assertEquals(result1, result2, "join() should be commutative");
  }

  @Test
  public void testJoin_ExpandedLocationsWithSameExpandedFrom() {
    var expandedFrom = List.of(
        new SourceLocation.DirectLocation(miniVadlPath,
            new SourceLocation.Position(30, 1),
            new SourceLocation.Position(30, 12)
        ),
        new SourceLocation.DirectLocation(miniVadlPath,
            new SourceLocation.Position(20, 4),
            new SourceLocation.Position(20, 15)
        )
    );
    var resultStart = new SourceLocation.Position(2, 1);
    var resultEnd = new SourceLocation.Position(10, 3);

    var locationA = SourceLocation.of(miniVadlPath,
        resultStart,
        new SourceLocation.Position(2, 3),
        expandedFrom
    );
    var locationB = SourceLocation.of(miniVadlPath,
        new SourceLocation.Position(10, 1),
        resultEnd,
        expandedFrom
    );

    var result1 = locationA.join(locationB);
    var result2 = locationB.join(locationA);

    assertEquals(
        SourceLocation.of(miniVadlPath, resultStart, resultEnd, expandedFrom),
        result1
    );
    assertEquals(result1, result2, "join() should be commutative");
  }

  @Test
  public void testJoin_ExpandedLocationsWithDifferentExpandedFrom() {
    var resultStart = new SourceLocation.Position(2, 1);
    var resultEnd = new SourceLocation.Position(20, 15);

    var expandedFrom1 = new SourceLocation.DirectLocation(miniVadlPath,
        new SourceLocation.Position(30, 1),
        new SourceLocation.Position(30, 12)
    );
    var expandedFrom2 = new SourceLocation.DirectLocation(miniVadlPath,
        new SourceLocation.Position(20, 4),
        resultEnd
    );

    var locationA = SourceLocation.of(miniVadlPath,
        resultStart,
        new SourceLocation.Position(2, 3),
        List.of(expandedFrom1)
    );
    var locationB = SourceLocation.of(miniVadlPath,
        // This primary location should be ignored
        new SourceLocation.Position(10, 1),
        new SourceLocation.Position(10, 3),

        List.of(expandedFrom2, expandedFrom1)
    );

    var result1 = locationA.join(locationB);
    var result2 = locationB.join(locationA);

    assertEquals(
        SourceLocation.of(miniVadlPath, resultStart, resultEnd, List.of(expandedFrom1)),
        result1
    );
    assertEquals(result1, result2, "join() should be commutative");
  }
}