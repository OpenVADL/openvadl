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

package vadl.iss.passes.opDecomposition;

import static org.assertj.core.api.Assertions.assertThat;
import static vadl.utils.GraphUtils.bits;

import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import vadl.types.BuiltInTable;
import vadl.viam.graph.Graph;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.passes.canonicalization.Canonicalizer;

public class IssOpDecompositionPassTest {

  @TestFactory
  Stream<DynamicTest> lsrTests() {
    return Stream.of(
        lsrSimple(0b01110000, 1, 8, 4),
        lsrSimple(0b110100, 6, 8, 4),
        lsrSimple(0b00000001, 1, 8, 4),  // Shift LSB
        lsrSimple(0b10000000, 7, 8, 4),  // Shift MSB
        lsrSimple(0b11111111, 8, 8, 4),  // Full shift
        lsrSimple(0b00000000, 3, 8, 4),  // All zeros
        lsrSimple(0b10101010, 2, 8, 4),  // Alternating bits
        lsrSimple(0b11110000, 4, 8, 4),  // High nibble
        lsrSimple(0b00001111, 4, 8, 4),  // Low nibble
        lsrSimple(0b11111111, 0, 8, 4),  // No shift

        lsrCplx(0b01110000, 1, 8, 0b111, 6, 3, 4),
        lsrCplx(0b10110100, 3, 8, 0b0001, 7, 4, 4),
        lsrCplx(0b00000001, 1, 8, 0b0, 5, 0, 4),  // Shift LSB
        lsrCplx(0b10000000, 7, 8, 0b1, 6, 0, 4),  // Shift MSB
        lsrCplx(0b11111111, 8, 8, 0b1, 4, 4, 4),  // Full shift
        lsrCplx(0b00000000, 3, 8, 0b0, 3, 2, 4),  // All zeros
        lsrCplx(0b10101010, 2, 8, 0b1010, 6, 2, 4),  // Alternating bits
        lsrCplx(0b11110000, 4, 8, 0b1111, 3, 0, 4),  // High nibble
        lsrCplx(0b00001111, 4, 8, 0b0, 5, 0, 4),  // Low nibble
        lsrCplx(0b11111111, 0, 8, 0b1111, 6, 3, 4),   // No shift

        lsrSimple(0b110100, 6, 8, 2),
        lsrSimple(0b10000000, 7, 8, 2),  // Shift MSB
        lsrSimple(0b11111111, 8, 8, 2),  // Full shift
        lsrSimple(0b00000000, 3, 8, 2),  // All zeros
        lsrSimple(0b10101010, 2, 8, 2),  // Alternating bits
        lsrSimple(0b11110000, 4, 8, 2),  // High nibble
        lsrSimple(0b00001111, 4, 8, 2),  // Low nibble
        lsrSimple(0b11111111, 0, 8, 2),  // No shift

        lsrCplx(0b10110100, 3, 8, 0b0001, 7, 4, 2),
        lsrCplx(0b10000000, 7, 8, 0b1, 6, 0, 2),  // Shift MSB
        lsrCplx(0b11111111, 8, 8, 0b1, 4, 4, 2),  // Full shift
        lsrCplx(0b00000000, 3, 8, 0b0, 3, 2, 2),  // All zeros
        lsrCplx(0b10101010, 2, 8, 0b1010, 6, 2, 2),  // Alternating bits
        lsrCplx(0b11110000, 4, 8, 0b1111, 3, 0, 2),  // High nibble
        lsrCplx(0b00001111, 4, 8, 0b0, 5, 0, 2),  // Low nibble
        lsrCplx(0b11111111, 0, 8, 0b1111, 6, 3, 2)   // No shift
    );
  }

  private DynamicTest lsrSimple(long a, int b, int width, int targetSize) {
    return DynamicTest.dynamicTest("LSR_" + a + "_" + b, () -> {
      var aN = bits(a, width).toNode();
      var bN = bits(b, width).toNode();
      var shift = BuiltInTable.LSR.call(aN, bN);
      testRequest(shift, targetSize);
    });
  }

  private DynamicTest lsrCplx(long a, int b, int width, int expected, int hi, int lo,
                              int targetSize) {
    return DynamicTest.dynamicTest("CPLX_LSR_" + a + "_" + b, () -> {
      var aN = bits(a, width).toNode();
      var bN = bits(b, width).toNode();
      var shift = BuiltInTable.LSR.call(aN, bN);
      testRequest(shift, hi, lo, expected, targetSize);
    });
  }


  private void testRequest(ExpressionNode expr, int hi, int lo, long expected, int targetSize) {
    var decomposed = new Decomposer(targetSize).request(expr.copy(), hi, lo);
    var testGraph = new Graph("test");
    testGraph.addWithInputs(decomposed);

    var decomposeResult = Canonicalizer.canonicalizeSubGraph(decomposed);

    assertThat(decomposeResult).isInstanceOf(ConstantNode.class);

    var decompVal = ((ConstantNode) decomposeResult).constant().asVal();
    assertThat(decompVal.longValue()).isEqualTo(expected);
  }

  private void testRequest(ExpressionNode expr, int targetSize) {
    var refGraph = new Graph("ref");
    var ref = refGraph.addWithInputs(expr.copy());
    var expectedResult = Canonicalizer.canonicalizeSubGraph(ref);

    assertThat(expectedResult).isInstanceOf(ConstantNode.class);

    var expectedVal = ((ConstantNode) expectedResult).constant().asVal().longValue();
    System.out.println(((ConstantNode) expectedResult).constant().asVal().binary());
    var hi = ref.type().asDataType().bitWidth() - 1;
    testRequest(ref, hi, 0, expectedVal, targetSize);
  }

}
