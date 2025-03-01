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

package vadl.viam;

import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vadl.AbstractTest;
import vadl.TestUtils;
import vadl.viam.passes.verification.ViamVerifier;

public class RelocationTest extends AbstractTest {

  @Test
  void testRelocation() throws IOException {
    var spec = runAndGetViamSpecification("unit/relocation/valid_relocations.vadl");
    ViamVerifier.verifyAllIn(spec);

    var test = TestUtils.findDefinitionByNameIn("Test", spec, InstructionSetArchitecture.class);
    var r1 = TestUtils.findDefinitionByNameIn("Test::R1", spec, Relocation.class);
    var f1 = TestUtils.findDefinitionByNameIn("Test::F1", spec, Function.class);

    Assertions.assertEquals(1, test.ownRelocations().size());
    Assertions.assertEquals(r1, test.ownRelocations().get(0));
    // relocations should not be added to functions, but hold separately
    Assertions.assertEquals(1, test.ownFunctions().size());
    Assertions.assertEquals(f1, test.ownFunctions().get(0));
  }

}
