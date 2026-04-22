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

import java.util.Objects;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import vadl.cli.decoder.mixin.CheckDecoderOptions;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.PassOrder;
import vadl.pass.PassOrders;

/**
 * The Command does provide the check subcommand.
 */
@Command(
    name = "check",
    description = "Verify the correctness of a VADL file without generating anything.",
    mixinStandardHelpOptions = true
)
public class CheckCommand extends BaseCommand implements Callable<Integer> {

  @CommandLine.Mixin
  final CheckDecoderOptions decoderOptions = new CheckDecoderOptions();

  @Override
  PassOrder passOrder(GeneralConfiguration configuration) {
    configuration.setDecoderOptions(
        decoderOptions.getDecoderOptions(Objects.requireNonNull(spec).commandLine()));
    return PassOrders.check(configuration);
  }
}
