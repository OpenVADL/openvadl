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

package vadl.cli;

import static picocli.CommandLine.ScopeType.INHERIT;

import java.io.IOException;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.PassOrder;
import vadl.pass.PassOrders;

/**
 * The Command does provide the rtl subcommand.
 */
@Command(
    name = "rtl",
    description = "Generate the RTL description (Chisel)",
    mixinStandardHelpOptions = true
)
public class RtlCommand extends BaseCommand {

  @CommandLine.Option(names = {"--dry-run"},
      scope = INHERIT,
      description = "Don't emit generated files.")
  boolean dryRun;

  @Override
  PassOrder passOrder(GeneralConfiguration configuration) throws IOException {
    configuration.setDryRun(dryRun);
    return PassOrders.rtl(configuration);
  }
}
