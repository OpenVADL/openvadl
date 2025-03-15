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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import vadl.gcb.valuetypes.TargetName;

/**
 * This record defines some gcb specific LCB configuration.
 */
public class GcbConfiguration extends GeneralConfiguration {
  @Nullable
  private TargetName targetName;

  public GcbConfiguration(GeneralConfiguration generalConfiguration,
                          @Nullable TargetName targetName) {
    super(generalConfiguration);
    this.targetName = targetName;
  }

  public static GcbConfiguration from(GeneralConfiguration generalConfiguration,
                                      @Nullable TargetName targetName) {
    return new GcbConfiguration(generalConfiguration, targetName);
  }

  public boolean isTargetNameNull() {
    return targetName == null;
  }

  @Nonnull
  public TargetName targetName() {
    if(targetName == null) {
      throw new RuntimeException("targetName must not be null");
    }

    return targetName;
  }

  public void setTargetName(@Nullable TargetName targetName) {
    this.targetName = targetName;
  }
}
