// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.iss.passes;

import java.io.IOException;
import javax.annotation.CheckForNull;
import vadl.configuration.IssConfiguration;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Specification;
import vadl.viam.UserModeEmulation;

/**
 * A specialized hardcoded rendering pass for QEMU User-Mode Emulation (UME) source files.
 */
public class UmeHardcodedRiscvDefinitionPass extends AbstractIssPass {
  public UmeHardcodedRiscvDefinitionPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("UME Hardcoded RISC-V Definition");
  }

  @CheckForNull
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    UserModeEmulation ume = UserModeEmulation.createDefault();
    viam.add(ume);
    return null;
  }
}
