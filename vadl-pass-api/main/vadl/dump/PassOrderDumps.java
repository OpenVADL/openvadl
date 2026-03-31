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

package vadl.dump;

import java.nio.file.Path;
import vadl.configuration.DumpMode;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.PassOrder;

/**
 * Utilities for adding dump passes to pass orders.
 */
public final class PassOrderDumps {
  private PassOrderDumps() {
  }

  /**
   * Appends a final HTML dump pass to the given pass order.
   */
  public static PassOrder addDump(PassOrder passOrder, String outPath) {
    var passSteps = passOrder.passSteps();
    var last = passSteps.get(passSteps.size() - 1);
    var config = new GeneralConfiguration(Path.of(outPath), DumpMode.ALWAYS);
    var dumpPass = new HtmlDumpPass(HtmlDumpPass.Config.from(config,
        last.pass().getName().value(),
        "This is a dump right after the pass " + last.key().value() + "."
    ));
    passOrder.add(dumpPass);
    return passOrder;
  }
}
