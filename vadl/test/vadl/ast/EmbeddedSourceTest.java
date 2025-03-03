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

package vadl.ast;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

public class EmbeddedSourceTest {

  @Test
  void testEmbeddedSource() {
    var prog = """
        instruction set architecture ISA = {}
        application binary interface ABI for ISA = {}
        micro processor MiP implements ISA with ABI = {
          source TestSource = -<{
            Hello, world!
          }>-
        }
        """;

    var ast = VadlParser.parse(prog);
    var mip = (MicroProcessorDefinition) ast.definitions.get(2);
    var source = (SourceDefinition) mip.definitions.get(0);
    assertThat(source.source.trim(), is("Hello, world!"));
  }
}
