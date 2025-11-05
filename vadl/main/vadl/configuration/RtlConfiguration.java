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

import java.util.Objects;
import javax.annotation.Nullable;

/**
 * Configuration values to control how RTL is generated and emitted.
 */
public class RtlConfiguration extends GeneralConfiguration {

  private String scalaPackage = "";

  private String scalaPackageDir = "src/main/scala/";

  private String scalaTestPackageDir = "src/test/scala/";

  private String scalaTestResourcesDir = "src/test/resources/";

  /**
   * Top module name. This is initialized in {@link vadl.rtl.passes.RtlConfigurationPass}.
   */
  @Nullable
  private String topModule = null;

  /**
   * Scala project name. This is initialized in {@link vadl.rtl.passes.RtlConfigurationPass}.
   */
  @Nullable
  private String projectName = null;

  /**
   * Enum to configure the external memory interface.
   *
   * <p>Async memory ignores the valid input and assumes reads/writes to always complete in
   * the same cycle.
   */
  public enum Memory { decoupled, async }

  private Memory memory = Memory.decoupled;

  @Nullable
  private String resetVector = null;

  private boolean keepSignals = false;

  private boolean emitDebugPrint = true;

  public RtlConfiguration(GeneralConfiguration generalConfig) {
    super(generalConfig);
  }

  public void setScalaPackage(String scalaPackage) {
    this.scalaPackage = scalaPackage;
  }

  public String getScalaPackage() {
    return scalaPackage;
  }

  /**
   * Sets the scala package and also updates package directory and test package directory
   * accordingly.
   *
   * @param scalaPackage scala package
   */
  public void setScalaPackageAndDirs(String scalaPackage) {
    this.scalaPackage = scalaPackage;
    this.scalaPackageDir = "src/main/scala/" + scalaPackage.replace('.', '/');
    this.scalaTestPackageDir = "src/test/scala/" + scalaPackage.replace('.', '/');
    this.scalaTestResourcesDir = "src/test/resources";
  }

  public String getScalaPackageDir() {
    return scalaPackageDir;
  }

  public String getScalaTestPackageDir() {
    return scalaTestPackageDir;
  }

  public String getScalaTestResourcesDir() {
    return scalaTestResourcesDir;
  }

  public void setTopModule(@Nullable String topModule) {
    this.topModule = topModule;
  }

  /**
   * Set top module name, if not already set.
   *
   * @param topModule top module name
   */
  public void setTopModuleIfEmpty(String topModule) {
    if (this.topModule == null) {
      this.topModule = topModule;
    }
  }

  public String getTopModule() {
    return Objects.requireNonNull(topModule);
  }

  public void setProjectName(@Nullable String projectName) {
    this.projectName = projectName;
  }

  /**
   * Set project name, if not already set.
   *
   * @param projectName project name
   */
  public void setProjectNameIfEmpty(String projectName) {
    if (this.projectName == null) {
      this.projectName = projectName;
    }
  }

  public String getProjectName() {
    return Objects.requireNonNull(projectName);
  }

  public Memory getMemory() {
    return memory;
  }

  public void setMemory(Memory memory) {
    this.memory = memory;
  }

  @Nullable
  public String getResetVector() {
    return resetVector;
  }

  public void setResetVector(@Nullable String resetVector) {
    this.resetVector = resetVector;
  }

  public boolean getKeepSignals() {
    return keepSignals;
  }

  public void setKeepSignals(boolean keepSignals) {
    this.keepSignals = keepSignals;
  }

  public boolean isEmitDebugPrint() {
    return emitDebugPrint;
  }

  public void setEmitDebugPrint(boolean emitDebugPrint) {
    this.emitDebugPrint = emitDebugPrint;
  }
}
