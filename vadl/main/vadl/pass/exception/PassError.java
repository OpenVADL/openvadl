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

package vadl.pass.exception;

import com.google.errorprone.annotations.FormatMethod;
import java.util.Arrays;
import javax.annotation.Nullable;

/**
 * This exception represents an error which happened during the pass execution.
 */
public class PassError extends RuntimeException {

  public PassError(String message) {
    super(message);
  }

  @FormatMethod
  public PassError(String message, @Nullable Object... args) {
    super(message.formatted(args));
  }

  /**
   * Removes the upper {@code n} stacktrace entries.
   * This is useful if helper methods create exceptions but are not
   * responsible for it.
   */
  public PassError shrinkStacktrace(int n) {
    StackTraceElement[] stackTrace = this.getStackTrace();
    if (stackTrace.length > n) {
      StackTraceElement[] newStackTrace = Arrays.copyOfRange(stackTrace, n, stackTrace.length);
      this.setStackTrace(newStackTrace);
    }
    return this;
  }
}
