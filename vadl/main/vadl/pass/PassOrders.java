// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.pass;

import java.io.IOException;
import javax.annotation.Nullable;
import vadl.configuration.GcbConfiguration;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.IssConfiguration;
import vadl.configuration.LcbConfiguration;
import vadl.configuration.RtlConfiguration;
import vadl.pass.order.CheckPassOrder;
import vadl.pass.order.GcbPassOrder;
import vadl.pass.order.IssPassOrder;
import vadl.pass.order.LcbPassOrder;
import vadl.pass.order.RtlPassOrder;
import vadl.pass.order.ViamPassOrder;
import vadl.viam.Specification;

/**
 * Compatibility facade for pass-order construction.
 */
public class PassOrders {
  public static PassOrder check(GeneralConfiguration configuration) {
    return CheckPassOrder.create(configuration);
  }

  public static PassOrder viam(GeneralConfiguration configuration) throws IOException {
    return ViamPassOrder.create(configuration);
  }

  public static PassOrder gcbAndCppCodeGen(GcbConfiguration configuration) throws IOException {
    return GcbPassOrder.create(configuration);
  }

  public static PassOrder lcb(LcbConfiguration configuration) throws IOException {
    return LcbPassOrder.create(configuration);
  }

  public static PassOrder iss(IssConfiguration configuration) throws IOException {
    return IssPassOrder.create(configuration);
  }

  public static PassOrder rtl(RtlConfiguration configuration) throws IOException {
    return RtlPassOrder.create(configuration);
  }

  /**
   * A pseudo pass that indicates the first pass in the PassOrder.
   */
  public static class ViamCreationPass extends Pass {
    public ViamCreationPass(GeneralConfiguration configuration) {
      super(configuration);
    }

    @Override
    public PassName getName() {
      return PassName.of("VIAM Creation (pseudo pass)");
    }

    @Nullable
    @Override
    public Object execute(PassResults passResults, Specification viam) throws IOException {
      return null;
    }
  }
}
