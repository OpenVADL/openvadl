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

package vadl.iss.passes.tcgLowering;

import java.io.IOException;
import javax.annotation.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * This pass only attaches a new {@link TcgCtx} to each instruction as an extension.
 * The TCG context is information per instruction that is used by several passes.
 */
public class IssTcgContextPass extends Pass {

  public IssTcgContextPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("ISS TCG Context Pass");
  }

  @Override
  public IssConfiguration configuration() {
    return (IssConfiguration) super.configuration();
  }

  @Override
  @Nullable
  public Object execute(PassResults passResults, Specification viam)
      throws IOException {

    var targetSize = configuration().targetSize();

    // Process each instruction in the ISA
    viam.isa().ifPresent(isa -> isa.ownInstructions()
        // attach new TCG context to each instruction
        .forEach(instr -> instr.attachExtension(new TcgCtx(instr.behavior(), targetSize))
        ));

    return null;
  }
}