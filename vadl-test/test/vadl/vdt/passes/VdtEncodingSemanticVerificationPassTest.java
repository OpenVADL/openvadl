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

package vadl.vdt.passes;

import io.github.rascmatt.z3.Z3Bootstrap;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.AbstractTest;
import vadl.TestUtils;
import vadl.configuration.DumpMode;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.IssConfiguration;
import vadl.error.Diagnostic;
import vadl.dump.PassFailureDumpHandler;
import vadl.pass.PassManager;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.vdt.model.Node;
import vadl.vdt.target.common.DecisionTreeStatsCalculator;
import vadl.vdt.target.dump.TextGraphGenerator;

public class VdtEncodingSemanticVerificationPassTest extends AbstractTest {

  private static final Logger log =
      LoggerFactory.getLogger(VdtEncodingSemanticVerificationPassTest.class);

  @Test
  void testBootstrapZ3() {
    Assertions.assertTrue(Z3Bootstrap.init(), "Expected Z3 library to be loaded successfully");
  }

  @Test
  void testDistinctEncoding_failsOverlappingEncoding() throws DuplicatedPassKeyException {

    /* GIVEN */
    final String vadl = """
        instruction set architecture TEST = {
        
          register X: Bits<5>
        
          format Format: Bits<8> =
          { a   [7..2]
          , b   [1..0]
          }
        
          instruction I1: Format = { }
          [ select when : b(1..0) = 0b00 || b(1..0) = 0b01 ]
          encoding I1 = { a = 0b000001 }
          assembly I1 = ( mnemonic )
        
          instruction I2: Format = { }
          [ select when : b(1..0) = 0b00 || b(1..0) = 0b10 ]
          encoding I2 = { a = 0b000001 }
          assembly I2 = ( mnemonic )
        
          instruction I3: Format = { }
          encoding I3 = { a = 0b000010 }
          assembly I3 = ( mnemonic )
        }
        """;

    var config =
        new IssConfiguration(new GeneralConfiguration(Path.of("build/test-output"), DumpMode.NONE));
    var spec = TestUtils.compileToViam(vadl);

    var manager = new PassManager(new PassFailureDumpHandler());
    manager.add(new VdtEncodingConstraintValidationPass(config));
    manager.add(new VdtInputPreparationPass(config));
    manager.add(new VdtEncodingSemanticVerificationPass(config));
    manager.add(new VdtLoweringPass(config));

    /* WHEN */
    Diagnostic error = Assertions.assertThrows(Diagnostic.class, () -> manager.run(spec));

    /* THEN */
    Assertions.assertTrue(error.getMessage().startsWith(
        "[ERROR] Overlapping instruction encoding detected: [I1, I2]. E.g. the encoding 0x4 "
            + "matches all listed encoding definitions."));
  }

  @Test
  void testDistinctEncoding_succeeds() throws DuplicatedPassKeyException, IOException {

    /* GIVEN */
    final String vadl = """
        instruction set architecture TEST = {
        
          register X: Bits<5>
        
          format Format: Bits<8> =
          { a   [7..4]
          , b   [3..0]
          }
        
          instruction I1: Format = { }
          [ select when : b(1..0) != 0b01 ]
          encoding I1 = { a = 0b0010 }
          assembly I1 = ( mnemonic )
        
          instruction I2: Format = { }
          [ select when : b(1..0) != 0b10 ]
          encoding I2 = { a = 0b0001 }
          assembly I2 = ( mnemonic )
        }
        """;

    var config =
        new IssConfiguration(new GeneralConfiguration(Path.of("build/test-output"), DumpMode.NONE));
    var spec = TestUtils.compileToViam(vadl);

    var manager = new PassManager(new PassFailureDumpHandler());
    manager.add(new VdtEncodingConstraintValidationPass(config));
    manager.add(new VdtInputPreparationPass(config));
    manager.add(new VdtEncodingSemanticVerificationPass(config));
    manager.add(new VdtLoweringPass(config));

    /* WHEN */
    manager.run(spec);

    /* THEN */
    var decodeTree = manager.getPassResults().lastResultOf(VdtLoweringPass.class, Node.class);

    Assertions.assertNotNull(decodeTree);

    log.info("VDT: {}", DecisionTreeStatsCalculator.statistics(decodeTree));
    log.info("Decoder: \n{}", new TextGraphGenerator(decodeTree).generate());
  }

}
