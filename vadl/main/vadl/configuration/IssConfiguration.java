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

import vadl.iss.passes.tcgLowering.Tcg_32_64;

/**
 * The configurations required to control the generation of the ISS (QEMU).
 * Some settings may be added by the {@link vadl.iss.passes.IssConfigurationPass}
 * if they are not statically available.
 */
public class IssConfiguration extends GeneralConfiguration {

  // is set by the IssConfigurationPass
  private String targetName;
  private boolean insnCount;
  private Tcg_32_64 targetSize;

  /**
   * Constructs a {@link IssConfiguration}.
   */
  public IssConfiguration(GeneralConfiguration generalConfig) {
    super(generalConfig);
    targetName = "unknown";
    insnCount = false;
    targetSize = Tcg_32_64.i64;
  }

  /**
   * Constructs IssConfiguration.
   *
   * @param insnCount used to determine if the iss generates add instruction for special
   *                  cpu register (QEMU)
   */
  public IssConfiguration(GeneralConfiguration generalConfig, boolean insnCount) {
    super(generalConfig);
    this.targetName = "unknown";
    this.insnCount = insnCount;
    targetSize = Tcg_32_64.i64;

  }

  public static IssConfiguration from(GeneralConfiguration generalConfig) {
    return new IssConfiguration(generalConfig);
  }

  public static IssConfiguration from(GeneralConfiguration generalConfig, boolean insnCounting) {
    return new IssConfiguration(generalConfig, insnCounting);
  }

  public String targetName() {
    return targetName;
  }

  public Tcg_32_64 targetSize() {
    return targetSize;
  }

  public void setTargetName(String targetName) {
    this.targetName = targetName;
  }

  public boolean isInsnCounting() {
    return insnCount;
  }

  public void setInsnCounting(boolean insnCounting) {
    this.insnCount = insnCounting;
  }

  public void setTargetSize(Tcg_32_64 targetSize) {
    this.targetSize = targetSize;
  }
}
