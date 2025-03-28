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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import vadl.types.Type;
import vadl.utils.SourceLocation;
import vadl.viam.graph.Graph;

class CompilerInstructionTest {

  Identifier first() {
    return new Identifier("firstIdentifier", SourceLocation.INVALID_SOURCE_LOCATION);
  }

  Identifier second() {
    return new Identifier("secondIdentifier", SourceLocation.INVALID_SOURCE_LOCATION);
  }

  CompilerInstruction create(Type type1, Type type2) {
    return new CompilerInstruction(
        new Identifier("test", SourceLocation.INVALID_SOURCE_LOCATION),
        new Parameter[] {new Parameter(first(), type1), new Parameter(second(), type2)},
        new Graph("graph")
    );
  }

  @Test
  void getLargestParameter_whenUnsignedAndSigned() {
    // Given
    var ty1 = Type.signedInt(32);
    var ty2 = Type.unsignedInt(32);

    var instruction = create(ty1, ty2);

    // When
    var parameter = instruction.getLargestParameter();

    // Then
    Assertions.assertThat(parameter.identifier.simpleName()).isEqualTo("secondIdentifier");
    Assertions.assertThat(parameter.type()).isEqualTo(ty2);
  }
}