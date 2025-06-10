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

package vadl.configuration;

import java.nio.file.Path;

/**
 * This configuration holds information for all passes.
 */
public class GeneralConfiguration {
  private final Path outputPath;
  private final boolean doDump;
  private boolean dryRun = false;


  public GeneralConfiguration(Path outputPath, boolean doDump) {
    this.outputPath = outputPath;
    this.doDump = doDump;
  }

  public GeneralConfiguration(GeneralConfiguration generalConfig) {
    this(generalConfig.outputPath, generalConfig.doDump);
    this.dryRun = generalConfig.dryRun;
  }

  public Path outputPath() {
    return outputPath;
  }

  public boolean doDump() {
    return doDump;
  }

  public boolean isDryRun() {
    return dryRun;
  }

  public void setDryRun(boolean dryRun) {
    this.dryRun = dryRun;
  }
}
