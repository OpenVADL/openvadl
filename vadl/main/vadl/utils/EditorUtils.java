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

package vadl.utils;

import java.util.Objects;
import javax.annotation.Nullable;

/**
 * A file to work around the "quirks" of some editors and IDE's.
 */
public class EditorUtils {

  @Nullable
  private static Boolean isIntelliJ = null;

  /**
   * Detect whether the program is currently executing inside IntelliJ.
   */
  public static boolean isIntelliJIDE() {
    if (isIntelliJ == null) {
      isIntelliJ = Objects.requireNonNullElse(System.getenv("TERMINAL_EMULATOR"), "")
          .equals("JetBrains-JediTerm")
          || Objects.requireNonNullElse(System.getenv("XPC_SERVICE_NAME"), "")
          .startsWith("application.com.jetbrains")
      ;
    }

    return isIntelliJ;
  }
}
