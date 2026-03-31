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

import javax.annotation.Nullable;
import vadl.gcb.valuetypes.TargetName;

/**
 * This record defines some lcb specific LCB configuration.
 */
public class LcbConfiguration extends GcbConfiguration {

  private boolean skipPatternGeneration = false;

  public LcbConfiguration(GeneralConfiguration gcbConfiguration, @Nullable TargetName targetName) {
    super(gcbConfiguration, targetName);
  }

  public LcbConfiguration(GeneralConfiguration gcbConfiguration, @Nullable TargetName targetName,
                          boolean skipPatternGeneration) {
    super(gcbConfiguration, targetName);
    this.skipPatternGeneration = skipPatternGeneration;
  }

  public static LcbConfiguration from(GcbConfiguration gcbConfiguration,
                                      TargetName targetName) {
    return new LcbConfiguration(gcbConfiguration, targetName);
  }

  public static LcbConfiguration from(GcbConfiguration gcbConfiguration,
                                      TargetName targetName, boolean skipPatternGeneration) {
    return new LcbConfiguration(gcbConfiguration, targetName, skipPatternGeneration);
  }

  public boolean skipPatternGeneration() {
    return skipPatternGeneration;
  }

}
