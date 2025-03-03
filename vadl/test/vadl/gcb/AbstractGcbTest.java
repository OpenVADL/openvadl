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

package vadl.gcb;

import java.io.IOException;
import vadl.configuration.GcbConfiguration;
import vadl.cppCodeGen.AbstractCppCodeGenTest;
import vadl.pass.PassKey;
import vadl.pass.PassOrder;
import vadl.pass.PassOrders;
import vadl.pass.exception.DuplicatedPassKeyException;

public abstract class AbstractGcbTest extends AbstractCppCodeGenTest {

  @Override
  public GcbConfiguration getConfiguration(boolean doDump) {
    return new GcbConfiguration(super.getConfiguration(doDump));
  }

  /**
   * Runs gcb passorder.
   *
   * @deprecated as {@link #setupPassManagerAndRunSpecUntil(String, PassOrder, PassKey)} is also
   *     deprecated.
   */
  @Deprecated
  public TestSetup runGcb(GcbConfiguration configuration,
                          String specPath,
                          PassKey until)
      throws IOException, DuplicatedPassKeyException {
    return setupPassManagerAndRunSpecUntil(specPath,
        PassOrders.gcbAndCppCodeGen(configuration), until);
  }
}