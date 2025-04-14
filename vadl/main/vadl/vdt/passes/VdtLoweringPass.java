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

package vadl.vdt.passes;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.AbstractIssPass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.vdt.impl.irregular.IrregularDecodeTreeGenerator;
import vadl.vdt.impl.irregular.model.DecodeEntry;
import vadl.vdt.model.Node;
import vadl.vdt.utils.BitPattern;
import vadl.vdt.utils.PBit;
import vadl.viam.Constant;
import vadl.viam.Encoding;
import vadl.viam.MicroProcessor;
import vadl.viam.Specification;
import vadl.viam.ViamError;

/**
 * Lowering pass that creates the VDT (VADL Decode Tree) from the VIAM definition.
 */
public class VdtLoweringPass extends AbstractIssPass {

  /**
   * Constructor for the VDT Lowering Pass.
   *
   * @param configuration the configuration
   */
  public VdtLoweringPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("VDT Lowering");
  }

  @Override
  public @Nullable Node execute(PassResults passResults, Specification viam)
      throws IOException {

    // TODO: handle inheritance correctly
    var isa = viam.mip().map(MicroProcessor::isa).orElse(null);
    if (isa == null) {
      throw new ViamError("No ISA found in the specification");
    }

    // TODO: get the byte order from the VADL specification
    final ByteOrder bo = ByteOrder.LITTLE_ENDIAN;

    var insns = isa.ownInstructions()
        .stream()
        .map(i -> {
          final BitPattern pattern = getInsnPattern(i, bo);
          // TODO: construct exclusion patterns from encoding constraints
          return new DecodeEntry(i, pattern.width(), pattern, Set.of());
        })
        .toList();

    return new IrregularDecodeTreeGenerator().generate(insns);
  }

  /**
   * Returns a bit pattern, where fixed bits in the instruction encoding are set to their respective
   * encoding value. All other bits are set to <i>don't care</i>.
   * <br>
   * The patterns will be constructed as the instructions appear in memory, i.e. in accordance with
   * the architecture's endianness.
   *
   * @param insn      The instruction
   * @param byteOrder The architecture's byte order
   * @return The bit pattern
   */
  private BitPattern getInsnPattern(vadl.viam.Instruction insn, ByteOrder byteOrder) {

    // Instruction definitions are in natural order (big endian), i.e. with the most significant
    // byte first.

    final PBit[] bits = new PBit[insn.format().type().bitWidth()];

    // Initialize all bits to "don't care"
    for (int i = 0; i < bits.length; i++) {
      bits[i] = new PBit(PBit.Value.DONT_CARE);
    }

    // Set fixed bits to their respective encoding value
    for (Encoding.Field encField : insn.encoding().fieldEncodings()) {
      BigInteger fixedValue = encField.constant().integer();
      List<Constant.BitSlice.Part> parts = encField.formatField().bitSlice().parts().toList();
      for (Constant.BitSlice.Part p : parts) {
        for (int i = p.lsb(); i <= p.msb(); i++) {
          var val = fixedValue.testBit(i - p.lsb()) ? PBit.Value.ONE : PBit.Value.ZERO;
          bits[bits.length - (i + 1)] = new PBit(val);
        }
      }
    }

    if (byteOrder != ByteOrder.LITTLE_ENDIAN) {
      // Pattern is already in the correct byte order
      return new BitPattern(bits);
    }

    if (bits.length % 8 != 0) {
      // TODO: handle misalignment gracefully
      throw new IllegalArgumentException(
          "Instruction format %s is not byte aligned.".formatted(insn.format()));
    }

    // Reverse the byte order
    for (int i = 0; i < bits.length / 16; i++) {
      for (int j = 0; j < 8; j++) {
        int l = i * 8 + j;
        int r = bits.length - (i + 1) * 8 + j;
        PBit tmp = bits[l];
        bits[l] = bits[r];
        bits[r] = tmp;
      }
    }

    return new BitPattern(bits);
  }
}
