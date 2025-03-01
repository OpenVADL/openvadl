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

package vadl.lcb.passes;

import java.io.IOException;
import java.util.HashSet;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * Dummy pass to add the {@link EncodeAssemblyImmediateAnnotation} to the instructions s.t.
 * they get printed correctly.
 */
public class DummyAnnotationPass extends Pass {
  public DummyAnnotationPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("DummyAnnotationPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var set = new HashSet<String>();
    set.add("AUIPC");
    set.add("BEQ");
    set.add("BGE");
    set.add("BGEU");
    set.add("BLT");
    set.add("BLTU");
    set.add("BNE");
    set.add("JAL");
    set.add("LUI");
    viam.isa().map(isa -> isa.ownInstructions().stream())
        .orElseGet(Stream::empty)
        .forEach(instruction -> {
          if (set.contains(instruction.identifier.simpleName())) {
            instruction.assembly().addAnnotation(new EncodeAssemblyImmediateAnnotation());
          }
        });

    return null;
  }
}
