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
import java.util.Objects;
import javax.annotation.Nullable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import vadl.cli.decoder.mixin.RtlDecoderOptions;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.RtlConfiguration;
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

  @CommandLine.Option(names = {"--memory"},
      scope = INHERIT,
      description = "Configure external memory interface: ${COMPLETION-CANDIDATES}",
      defaultValue = "decoupled")
  RtlConfiguration.Memory memory = RtlConfiguration.Memory.decoupled;

  @CommandLine.Option(names = {"--scala-package"},
      scope = INHERIT,
      description = "Package to emit scala code in.",
      defaultValue = "")
  String scalaPackage = "";

  @CommandLine.Option(names = {"--top-module"},
      scope = INHERIT,
      description = "Override the top module name. By default, this is the processor name from the "
          + "specification.")
  @Nullable
  String topModule = null;

  @CommandLine.Option(names = {"--project-name"},
      scope = INHERIT,
      description = "Override the project name. By default, this is the basename of the "
          + "specification file.")
  @Nullable
  String projectName = null;

  @CommandLine.Option(names = {"--reset-vector"},
      scope = INHERIT,
      description = "Read the reset vector from an external signal with this name. "
          + "Useful for test benches. "
          + "Overrides any reset value for the PC in the specification.")
  @Nullable
  String resetVector = null;

  @CommandLine.Option(names = {"--keep-signals"},
      scope = INHERIT,
      description = "Marks signals in generated HDL to not be optimized or removed during "
          + "synthesis and simulation.",
      defaultValue = "false")
  boolean keepSignals = false;

  @CommandLine.Option(names = {"--rvfi"},
      scope = INHERIT,
      description = "Emit outputs for the RISC-V Formal Interface.",
      defaultValue = "false")
  boolean emitRVFI = false;

  @CommandLine.Option(names = {"--dry-run"},
      scope = INHERIT,
      description = "Don't emit generated files.")
  boolean dryRun;

  @CommandLine.Mixin
  final RtlDecoderOptions decoderOptions = new RtlDecoderOptions();

  @Override
  PassOrder passOrder(GeneralConfiguration configuration) throws IOException {
    var rtlConfig = new RtlConfiguration(configuration);
    rtlConfig.setMemory(memory);
    rtlConfig.setResetVector(resetVector);
    rtlConfig.setKeepSignals(keepSignals);
    rtlConfig.setScalaPackageAndDirs(scalaPackage);
    rtlConfig.setTopModule(topModule);
    rtlConfig.setProjectName(projectName);
    rtlConfig.setEmitRVFI(emitRVFI);
    rtlConfig.setDryRun(dryRun);
    rtlConfig.setDecoderOptions(
        decoderOptions.getDecoderOptions(Objects.requireNonNull(spec).commandLine()));
    return PassOrders.rtl(rtlConfig);
  }
}
