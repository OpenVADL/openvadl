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

import java.util.Map;
import vadl.template.Renderable;

public class RtlConfiguration extends GeneralConfiguration implements Renderable {

  private String scalaPackage = "";

  private String scalaPackageDir = "src/main/scala/";

  private String scalaTestPackageDir = "src/test/scala/";

  private String topModule = "Core";

  private String projectName = "Core";

  public RtlConfiguration(GeneralConfiguration generalConfig) {
    super(generalConfig);
  }

  public void setScalaPackage(String scalaPackage) {
    this.scalaPackage = scalaPackage;
    this.scalaPackageDir = "src/main/scala/" + scalaPackage.replace('.', '/');
    this.scalaTestPackageDir = "src/test/scala/" + scalaPackage.replace('.', '/');
  }

  public String getScalaPackage() {
    return scalaPackage;
  }

  public String getScalaPackageDir() {
    return scalaPackageDir;
  }

  public String getScalaTestPackageDir() {
    return scalaTestPackageDir;
  }

  public void setTopModule(String topModule) {
    this.topModule = topModule;
  }

  public String getTopModule() {
    return topModule;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public String getProjectName() {
    return projectName;
  }

  @Override
  public Map<String, Object> renderObj() {
    return Map.of(
        "scalaPackage", scalaPackage,
        "scalaPackageDir", scalaPackageDir,
        "scalaTestPackageDir", scalaTestPackageDir,
        "topModule", topModule,
        "projectName", projectName
    );
  }
}
