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

package vadl.vdt.impl.irregular;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.AbstractTest;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.IssConfiguration;
import vadl.pass.PassManager;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.vdt.model.Node;
import vadl.vdt.passes.VdtLoweringPass;
import vadl.vdt.target.common.DecisionTreeDecoder;
import vadl.vdt.target.common.DecisionTreeStatsCalculator;
import vadl.vdt.utils.BitVector;
import vadl.vdt.utils.Instruction;

public class VariableLengthRiscTest extends AbstractTest {

  private static final Logger log = LoggerFactory.getLogger(VariableLengthRiscTest.class);

  @Test
  void testGenerateVDT() throws IOException, DuplicatedPassKeyException {

    /* GIVEN */

    var config = new GeneralConfiguration(Path.of("build/test-output"), true);
    var spec = runAndGetViamSpecification("sys/v-risc/VarRisc.vadl");

    var passManager = new PassManager();
    passManager.add(new VdtLoweringPass(new IssConfiguration(config)));

    /* WHEN */
    passManager.run(spec);

    /* THEN */

    var result = passManager.getPassResults();
    var decodeTree = result.lastResultOf(VdtLoweringPass.class, Node.class);

    Assertions.assertNotNull(decodeTree);

    log.info("VDT: {}", DecisionTreeStatsCalculator.statistics(decodeTree));

    var decoder = new DecisionTreeDecoder(decodeTree);

    // TYPE_I:

    // ADDI: opcode: 62, func5: 0
    assertDecision(decoder, "00111110 00000000 00000000 00000000", "ADDI");
    // SUBI: opcode: 62, func5: 1
    assertDecision(decoder, "01111110 00000000 00000000 00000000", "SUBI");
    // MULI: opcode: 62, func5: 2
    assertDecision(decoder, "10111110 00000000 00000000 00000000", "MULI");

    // TYPE_S:

    // ADD_S: opcode: 0
    assertDecision(decoder, "00000000 00000000", "ADD_S");
    // MOV: opcode: 22
    assertDecision(decoder, "00010110 00000000", "MOV");

    // TYPE_R:

    // opcode: 63, func9: 21
    assertDecision(decoder, "01111111 00000101 00000000 00000000", "ULE");

    // TYPE_L:

    // opcode: 60, func5: 22
    assertDecision(decoder, "00111100 10110000 00000000 00000000 00000000 00000000", "SGTI_L");

    // opcode: 60, func5: 17
    assertDecision(decoder, "00111100 1000100 00000000 00000000 00000000 00000000", "SNEI_L");
  }

  private void assertDecision(DecisionTreeDecoder decoder, String insn, String expectedName) {
    insn = insn.replace(" ", "");
    Instruction decision = decoder.decide(BitVector.fromString(insn, insn.length()));
    Assertions.assertNotNull(decision);
    Assertions.assertEquals(expectedName, decision.source().simpleName());
  }

}
