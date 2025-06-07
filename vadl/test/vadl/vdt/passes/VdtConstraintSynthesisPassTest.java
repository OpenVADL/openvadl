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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vadl.TestUtils;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassManager;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.vdt.impl.irregular.model.DecodeEntry;
import vadl.vdt.impl.irregular.model.ExclusionCondition;

public class VdtConstraintSynthesisPassTest {

  private static final String ISA = """
      instruction set architecture TEST = {
      
        register X: Bits<5>
      
        format Format: Bits<8> =
        { a  [7]
        , b  [6]
        , c  [5..4]
        , d  [3..2]
        , e  [1..0]
        }
      
        instruction I1: Format = { }
        instruction I2: Format = { }
        instruction I3: Format = { }
        instruction I4: Format = { }
        instruction I5: Format = { }
        instruction I6: Format = { }
        instruction I7: Format = { }
      
      
        encoding I1 = { a = 0b0, b = 0b0 }
        encoding I2 = { a = 0b0, b = 0b1 }
        encoding I3 = { a = 0b1, b = 0b0, e = 0b00 }
        encoding I4 = { a = 0b1, b = 0b0, e = 0b01 }
        encoding I5 = { a = 0b0, b = 0b0, c = 0b00, e = 0b01 }
        encoding I6 = { a = 0b0, b = 0b0, c = 0b00, e = 0b10 }
        encoding I7 = { a = 0b0, c = 0b11 }
      
        assembly I1 = ( mnemonic )
        assembly I2 = ( mnemonic )
        assembly I3 = ( mnemonic )
        assembly I4 = ( mnemonic )
        assembly I5 = ( mnemonic )
        assembly I6 = ( mnemonic )
        assembly I7 = ( mnemonic )
      }
      """;

  @Test
  void testSynthesizeConstraints() throws DuplicatedPassKeyException, IOException {
    /* GIVEN */
    var spec = TestUtils.compileToViam(ISA);
    var config = new GeneralConfiguration(Path.of("build/test-output"), false);

    var passManager = new PassManager();
    passManager.add(new VdtInputPreparationPass(config));
    passManager.add(new VdtConstraintSynthesisPass(config));

    /* WHEN */
    passManager.run(spec);

    /* THEN */
    List<DecodeEntry> decodeEntries = getResult(passManager, VdtConstraintSynthesisPass.class);

    Assertions.assertEquals(7, decodeEntries.size());

    Assertions.assertEquals("I1", decodeEntries.getFirst().source().simpleName());
    Assertions.assertEquals(2, decodeEntries.getFirst().exclusionConditions().size());

    final List<ExclusionCondition> exclusions =
        new ArrayList<>(decodeEntries.getFirst().exclusionConditions());
    exclusions.sort(Comparator.comparing(e -> e.matching().toString()));

    Assertions.assertEquals("--00--01", exclusions.getFirst().matching().toString());
    Assertions.assertEquals("--00--10", exclusions.getLast().matching().toString());

    for (int i = 1; i < decodeEntries.size(); i++) {
      Assertions.assertEquals("I" + (i + 1), decodeEntries.get(i).source().simpleName());
      Assertions.assertEquals(0, decodeEntries.get(i).exclusionConditions().size());
    }
  }

  @SuppressWarnings("unchecked")
  private <T, U extends Pass> T getResult(PassManager passManager, Class<U> passType) {
    return (T) passManager.getPassResults().lastResultOf(passType);
  }
}
