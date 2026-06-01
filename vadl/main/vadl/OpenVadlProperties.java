// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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
  private static String commit;
  private static String commitDate;

  static {
    try (InputStream input = OpenVadlProperties.class.getResourceAsStream(VERSION_PROPERTIES)) {
      if (input == null) {
        version = "unknown";
        commit = "unknown";
        commitDate = "unknown";
      } else {
        Properties prop = new Properties();
        prop.load(input);
        version = prop.getProperty("version", "unknown");
        commit = prop.getProperty("commit", "unknown");
        commitDate = prop.getProperty("commit.date", "unknown");
      }
    } catch (IOException ex) {
      version = "unknown";
      commit = "unknown";
      commitDate = "unknown";
    }
  }

  public static String getVersion() {
    return version;
  }

  public static String getCommit() {
    return commit;
  }

  public static String getCommitDate() {
    return commitDate;
  }
}
