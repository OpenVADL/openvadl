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

package vadl.cli;

import java.util.concurrent.Callable;
import javax.annotation.Nullable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import vadl.lsp.LspEntryPoint;
import vadl.lsp.VadlLanguageServer.Settings;

/**
 * The Command provides the lsp subcommand.
 */
@Command(
    name = "lsp",
    description = "Start the OpenVADL Language Server.",
    mixinStandardHelpOptions = true
)
public class LspCommand implements Callable<Integer> {

  @Option(names = {"-p", "--port"},
      description =
          "TCP port on which to listen. If not given: Communicate via stdin/stdout instead",
      defaultValue = Option.NULL_VALUE)
  @Nullable
  Integer port;

  @Option(names = "--no-syntax-highlighting",
      description = "Disable server-based syntax highlighting")
  boolean noSyntaxHighlighting;

  @Override
  public Integer call() {
    var settings = new Settings(noSyntaxHighlighting);
    return LspEntryPoint.run(port, settings);
  }
}
