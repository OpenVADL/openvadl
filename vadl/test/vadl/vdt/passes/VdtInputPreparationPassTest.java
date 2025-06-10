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
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vadl.TestUtils;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.PassManager;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.vdt.impl.irregular.model.DecodeEntry;
import vadl.vdt.impl.irregular.model.ExclusionCondition;
import vadl.vdt.utils.BitPattern;

public class VdtInputPreparationPassTest {

  private static String wrapProg(String formulas) {
    return """
        instruction set architecture TEST = {
        
          register X: Bits<5>
        
          format Format: Bits<32> =
          { one   [31..15]
          , two   [14..10]
          , three [9]
          , four  [8..0]
          , accFunc = one as SInt
          }
        
          instruction Instr: Format = { }
          [ select when : %s ]
          encoding Instr = { three = 0b1 }
          assembly Instr = ( mnemonic )
        }
        """.formatted(formulas);
  }

  @Test
  @SuppressWarnings("unchecked")
  void testPrepareEncoding() throws DuplicatedPassKeyException, IOException {
    /* GIVEN */
    var formula = "one(2,0) != 2 || two = 1";
    var spec = TestUtils.compileToViam(wrapProg(formula));
    var config = new GeneralConfiguration(Path.of("build/test-output"), false);

    var passManager = new PassManager();
    passManager.add(new VdtInputPreparationPass(config));

    /* WHEN */
    passManager.run(spec);

    /* THEN */
    List<DecodeEntry> decodeEntries =
        (List<DecodeEntry>) passManager.getPassResults()
            .lastResultOf(VdtInputPreparationPass.class);

    Assertions.assertEquals(1, decodeEntries.size());

    DecodeEntry entry = decodeEntries.getFirst();
    Assertions.assertEquals(1, entry.exclusionConditions().size());

    // Patterns are in little-endian byte order
    ExclusionCondition exclusion = entry.exclusionConditions().iterator().next();
    Assertions.assertEquals("--------0-------------1---------",
        exclusion.matching().toString());

    Assertions.assertEquals(1, exclusion.unmatching().size());
    BitPattern unmatching = exclusion.unmatching().iterator().next();

    Assertions.assertEquals("---------00001------------------",
        unmatching.toString());
  }
}
