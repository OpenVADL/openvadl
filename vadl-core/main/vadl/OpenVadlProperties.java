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

package vadl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Provides API to access the properties in {@code open-vadl.properties} which is
 * generated and bundled at build time.
 * It contains project information such as the current version.
 */
public class OpenVadlProperties {
  private static final String VERSION_PROPERTIES = "/open-vadl.properties";
  private static String version;

  static {
    try (InputStream input = OpenVadlProperties.class.getResourceAsStream(VERSION_PROPERTIES)) {
      if (input == null) {
        version = "unknown";
      } else {
        Properties prop = new Properties();
        prop.load(input);
        version = prop.getProperty("version", "unknown");
      }
    } catch (IOException ex) {
      version = "unknown";
    }
  }

  public static String getVersion() {
    return version;
  }
}
