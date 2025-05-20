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

package vadl.vdt.target.common;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vadl.AbstractTest;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.IssConfiguration;
import vadl.pass.PassManager;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.types.DataType;
import vadl.utils.FieldExtractionUtils;
import vadl.vdt.model.Node;
import vadl.vdt.passes.VdtLoweringPass;
import vadl.vdt.target.common.dto.DecodedInstruction;
import vadl.viam.Constant.Value;
import vadl.viam.Instruction;

public class DecisionTreeDecoderTest extends AbstractTest {

  @Test
  void testEvaluateAccessFunction() throws DuplicatedPassKeyException, IOException {

    /* GIVEN */

    // Parse input spec and create the decoder tree
    var spec = runAndGetViamSpecification("sys/risc-v/rv64im.vadl");

    var config =
        new IssConfiguration(new GeneralConfiguration(Path.of("build/test-output"), true));

    var passManager = new PassManager();
    passManager.add(new VdtLoweringPass(config));
    passManager.run(spec);

    var decodeTree = passManager.getPassResults().lastResultOf(VdtLoweringPass.class, Node.class);
    var decoder = new DecisionTreeDecoder(decodeTree);

    // bne x0, x0, 1234
    // 01001100000000000001100101100011
    var encoding = Value.fromInteger(
        new BigInteger("01001100000000000001100101100011", 2),
        DataType.unsignedInt(32));

    /* WHEN */

    DecodedInstruction decoded = decoder.decode(encoding);

    /* THEN */

    // Check that the instruction was decoded correctly
    Assertions.assertNotNull(decoded);
    Assertions.assertEquals("BNE", decoded.source().simpleName());

    var imm = Arrays.stream(decoded.source().format().fields())
        .filter(f -> "imm".equals(f.simpleName()))
        .findFirst().orElse(null);
    Assertions.assertNotNull(imm);

    // Extract the immediate field (617)
    Value immValue = decoded.get(imm);
    Assertions.assertNotNull(immValue);
    Assertions.assertEquals(immValue.integer(), BigInteger.valueOf(617));
  }

  @Test
  void testEvaluateAccessFunction_littleEndian() {

    /* GIVEN */
    var spec = runAndGetViamSpecification("sys/risc-v/rv64im.vadl");
    Assertions.assertNotNull(spec);

    var isa = spec.isa().orElse(null);
    Assertions.assertNotNull(isa);

    var btype = isa.ownInstructions().stream()
        .filter(i -> "BNE".equals(i.simpleName()))
        .map(Instruction::format)
        .findAny().orElse(null);
    Assertions.assertNotNull(btype);

    var imm = Arrays.stream(btype.fields())
        .filter(f -> "imm".equals(f.simpleName()))
        .findFirst().orElse(null);
    Assertions.assertNotNull(imm);

    var immS = btype.fieldAccesses().stream().findFirst().orElse(null);
    Assertions.assertNotNull(immS);

    // bne x0, x0, 1234
    // 01001100000000000001100101100011
    // -> Swap byte order for LE encoding
    // 01100011 00011001 00000000 01001100
    var encoding = new BigInteger("01100011000110010000000001001100", 2);

    /* WHEN */

    // First, extract the raw immediate '617'
    Value immValue = FieldExtractionUtils.extract(encoding, ByteOrder.LITTLE_ENDIAN, imm);

    /* THEN */
    Assertions.assertNotNull(immValue);
    Assertions.assertEquals(immValue.integer(), BigInteger.valueOf(617));
  }

}