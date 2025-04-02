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


import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * The VADL CLI entry class.
 */
@Command(mixinStandardHelpOptions = true,
    name = "OpenVADL",
    description = "The OpenVadl CLI tool.",
    versionProvider = VersionProvider.class,
    subcommands = {CheckCommand.class, IssCommand.class, LcbCommand.class, RtlCommand.class})
public class Main implements Runnable {
  @Override
  public void run() {
    new CommandLine(new Main()).usage(System.out);
  }

  /**
   * Entry method for the openvadl CLI application.
   */
  public static void main(String[] args) {
    int exitCode =
        new CommandLine(new Main())
            .setCaseInsensitiveEnumValuesAllowed(true)
            .execute(args);
    System.exit(exitCode);
  }
}