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

package vadl.error;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A general exception that something went wrong with a list of errors that caused the problem.
 */
public class DiagnosticList extends RuntimeException {
  public List<Diagnostic> items;

  public DiagnosticList(List<Diagnostic> items) {
    super(items.size() + " diagnostics(s):\n" + buildErrorMessage(items));
    this.items = items;
  }

  private static String buildErrorMessage(List<Diagnostic> errors) {
    return errors.stream().map(Diagnostic::getMessage).collect(Collectors.joining("\n"));
  }
}
