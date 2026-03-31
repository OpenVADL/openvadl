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
import javax.annotation.Nullable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.LcbConfiguration;
import vadl.gcb.valuetypes.TargetName;
import vadl.pass.PassOrder;
import vadl.pipeline.LcbGcbPassOrders;

/**
 * The Command does provide the lcb subcommand.
 */
@Command(
    name = "lcb",
    description = "Generate the LCB (LLVM Compiler Backend)",
    mixinStandardHelpOptions = true
)
public class LcbCommand extends BaseCommand {

  @LazyInit
  @Option(names = {"-t",
      "--target"}, scope = INHERIT, description = "Target Name")
  String targetName;

  @Option(names = {"--skip-pattern-generation"}, scope = INHERIT,
      description = "Skip TableGen pattern generation")
  boolean skipPatternGeneration = false;

  @Override
  PassOrder passOrder(GeneralConfiguration configuration) throws IOException {
    var lcbConfig = new LcbConfiguration(configuration, targetName(), skipPatternGeneration);
    return LcbGcbPassOrders.lcb(lcbConfig);
  }

  @Nullable
  private TargetName targetName() {
    if (targetName != null) {
      return new TargetName(targetName);
    } else {
      return null;
    }
  }
}
