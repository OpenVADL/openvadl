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
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import vadl.configuration.GcbConfiguration;
import vadl.cppCodeGen.AbstractCppCodeGenTest;
import vadl.gcb.valuetypes.TargetName;
import vadl.pass.PassKey;
import vadl.pass.PassOrder;
import vadl.pass.PassOrders;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.viam.Format;
import vadl.viam.Instruction;
import vadl.viam.Specification;

public abstract class AbstractGcbTest extends AbstractCppCodeGenTest {

  @Override
  public GcbConfiguration getConfiguration(boolean doDump) {
    return new GcbConfiguration(super.getConfiguration(doDump),
        new TargetName("processorNameValue"));
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

  /**
   * Helper method to get an {@link Instruction} from a {@link Specification}.
   */
  @Nullable
  protected Instruction getInstrByName(String instruction,
                                       Specification specification) {
    return specification.isa().stream().flatMap(x -> x.ownInstructions().stream())
        .filter(x -> x.simpleName().equals(instruction))
        .findFirst()
        .get();
  }

  /**
   * Helper method to extract an immediate.
   */
  protected @Nonnull Optional<Format.Field> getImmediate(String imm,
                                                         List<Format.Field> immediates) {
    return immediates.stream().filter(x -> x.identifier.simpleName().equals(imm)).findFirst();
  }
}