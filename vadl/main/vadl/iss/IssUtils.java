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

package vadl.iss;

import com.google.errorprone.annotations.FormatMethod;
import vadl.error.Diagnostic;
import vadl.viam.graph.Node;

/**
 * Iss specific utility methods.
 * Some more TCG related methods can be found in {@link vadl.iss.passes.TcgPassUtils}.
 */
public class IssUtils {

  /**
   * Throws a user-readable internal error with a call to report the issue.
   */
  @FormatMethod
  @SuppressWarnings("DoNotCallSuggester")
  public static void internalError(Node node, String format, Object... args) {
    throw Diagnostic.error("INTERNAL ERROR", node)
        .description(format, args)
        .help("Please report this issue at https://github.com/openvadl/openvadl/issues")
        .build();
  }

}