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

import com.google.errorprone.annotations.concurrent.LazyInit;
import java.io.IOException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.LcbConfiguration;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.pass.PassOrder;
import vadl.pass.PassOrders;

/**
 * The Command does provide the iss subcommand.
 */
@Command(
    name = "lcb",
    description = "Generate the LCB (LLVM Compiler Backend)",
    mixinStandardHelpOptions = true
)
public class LcbCommand extends BaseCommand {

  @LazyInit
  @Option(names = {"-p",
      "--process"}, required = true, scope = INHERIT, description = "Processor Name")
  String processorName;

  @Override
  PassOrder passOrder(GeneralConfiguration configuration) throws IOException {
    var issConfig = new LcbConfiguration(configuration, new ProcessorName(processorName));
    return PassOrders.lcb(issConfig);
  }
}
