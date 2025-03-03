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

package vadl.lcb;

import java.io.IOException;
import java.util.List;
import vadl.configuration.LcbConfiguration;
import vadl.cppCodeGen.AbstractCppCodeGenTest;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.pass.Pass;
import vadl.pass.PassKey;
import vadl.pass.PassOrder;
import vadl.pass.PassOrders;
import vadl.pass.exception.DuplicatedPassKeyException;

public abstract class AbstractLcbTest extends AbstractCppCodeGenTest {

  @Override
  public LcbConfiguration getConfiguration(boolean doDump) {
    return new LcbConfiguration(super.getConfiguration(doDump),
        new ProcessorName("processorNameValue"));
  }

  /**
   * Runs lcb pass order.
   *
   * @deprecated as {@link #setupPassManagerAndRunSpecUntil(String, PassOrder, PassKey)} is also
   *     deprecated.
   */
  @Deprecated
  public TestSetup runLcb(LcbConfiguration configuration,
                          String specPath,
                          PassKey until)
      throws IOException, DuplicatedPassKeyException {
    return setupPassManagerAndRunSpecUntil(specPath,
        PassOrders.lcb(configuration), until);
  }


  public TestSetup runLcb(LcbConfiguration configuration,
                          String specPath)
      throws IOException, DuplicatedPassKeyException {
    return setupPassManagerAndRunSpec(specPath,
        PassOrders.lcb(configuration));
  }

  /**
   * Inject a temporary {@link Pass} into the {@link PassOrder}.
   *
   * @param after which pass the {@code pass} should be scheduled.
   * @param pass  to be scheduled.
   */
  public record TemporaryTestPassInjection(Class<?> after, Pass pass) {

  }

  /**
   * Sometimes it is required to have additional passes during test execution. However,
   * these passes are not in the default order. With the {@code temporaryPasses} argument,
   * the function caller can specify what passes and when to schedule them.
   *
   * @deprecated as {@link #setupPassManagerAndRunSpecUntil(String, PassOrder, PassKey)} is also
   *     deprecated.
   */
  @Deprecated
  public TestSetup runLcb(LcbConfiguration configuration,
                          String specPath,
                          PassKey until,
                          List<TemporaryTestPassInjection> temporaryPasses)
      throws IOException, DuplicatedPassKeyException {
    var passOrder = PassOrders.lcb(configuration);
    for (var tempPass : temporaryPasses) {
      passOrder.addAfterLast(tempPass.after, tempPass.pass);
    }
    return setupPassManagerAndRunSpecUntil(specPath,
        passOrder, until);
  }
}
