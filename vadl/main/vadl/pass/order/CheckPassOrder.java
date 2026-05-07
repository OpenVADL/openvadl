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

package vadl.pass.order;

import vadl.configuration.GeneralConfiguration;
import vadl.pass.PassOrder;
import vadl.pass.PassOrders;
import vadl.viam.passes.verification.ViamVerificationPass;

/**
 * Builds the pass order used by the `check` command.
 */
public final class CheckPassOrder {
  private CheckPassOrder() {
  }

  /**
   * Creates the pass order.
   */
  public static PassOrder create(GeneralConfiguration configuration) {
    var order = new PassOrder();
    order.add(new PassOrders.ViamCreationPass(configuration));

    OrderSupport.addHtmlDump(order, configuration, "VIAM Creation",
        "Dump directly after frontend generated VIAM.");

    order.add(new ViamVerificationPass(configuration));
    OrderSupport.addDecodePasses(order, configuration);
    OrderSupport.addHtmlDump(order, configuration,
        "VDT Creation",
        "Dump directly after VDT generation.");
    return order;
  }
}
