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

import java.util.LinkedHashSet;
import java.util.List;

/**
 * A storage where diagnostics are added to which shouldn't interrupt the program flow but which
 * should nonetheless be presented to the programmer at the end.
 *
 * <p>In case you want to interrupt the program flow you can simply throw a {@link Diagnostic} or
 * a {@link DiagnosticList}.
 */
public class DeferredDiagnosticStore {
  private static final LinkedHashSet<Diagnostic> diagnosticList = new LinkedHashSet<>();

  /**
   * Add another diagnostic to the store.
   *
   * <p>If the diagnostic is already in the store it won't be inserted a second time.
   */
  public static void add(Diagnostic diagnostic) {
    diagnosticList.add(diagnostic);
  }

  /**
   * Add another diagnostic to the store.
   *
   * <p>If the diagnostic is already in the store it won't be inserted a second time.
   */
  public static void add(DiagnosticBuilder diagnostic) {
    diagnosticList.add(diagnostic.build());
  }

  /**
   * Retrieves all diagnostics in the order they were inserted.
   */
  public static List<Diagnostic> getAll() {
    return diagnosticList.stream().toList();
  }

  public static boolean isEmpty() {
    return diagnosticList.isEmpty();
  }
}
