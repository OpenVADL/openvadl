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

package vadl.vdt.utils;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vadl.vdt.impl.irregular.model.DecodeEntry;
import vadl.viam.Identifier;
import vadl.viam.Instruction;
import vadl.viam.graph.Graph;

public class SequentialInstructionDecoderTest {

  @ParameterizedTest
  @MethodSource("testSource")
  void testMatchInsn(BitPattern pattern, BitVector encoding) {

    /* GIVEN */
    var decoder = new SequentialInstructionDecoder(List.of(
        toInsn("i", pattern)
    ));

    /* WHEN */
    List<DecodeEntry> result = decoder.decode(encoding);

    /* THEN */
    Assertions.assertEquals(1, result.size());
    Assertions.assertEquals("i", result.getFirst().source().simpleName());
  }

  static Stream<Arguments> testSource() {
    return Stream.of(
        // Pattern should be extended to match the prefix of the encoding
        Arguments.of(
            BitPattern.fromBitVector(
                BitVector.fromValue(new BigInteger("00000e0f", 16), 32),
                BitVector.fromValue(new BigInteger("00008052", 16), 32)
            ),
            BitVector.fromValue(new BigInteger("882a805200006000", 16), 64)
        ),
        // Pattern should be truncated so only its prefix is matched against a shorter insn
        Arguments.of(
            BitPattern.fromBitVector(
                BitVector.fromValue(new BigInteger("00000e0f", 16), 32),
                BitVector.fromValue(new BigInteger("00008052", 16), 32)
            ),
            BitVector.fromValue(new BigInteger("000080", 16), 24)
        )
    );
  }

  private DecodeEntry toInsn(String name, BitPattern pattern) {
    var i = new Instruction(Identifier.noLocation(name), new Graph("behavior"), null, null);
    return new DecodeEntry(i, pattern.width(), pattern, Set.of());
  }
}
