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

import java.net.URI;
import javax.annotation.Nullable;
import vadl.viam.Specification;

/**
 * This interface allows VIAM tests to access an unknown frontend.
 * This allows to define tests in open-vadl while executing them from the old vadl project.
 *
 * <p>The old vadl implements the interface and sets the
 * {@link Provider#globalProvider} before executing the tests in the
 * {@link vadl.test} package.</p>
 */
public interface TestFrontend {

  /**
   * Runs the specification until AST to VIAM conversion is done.
   *
   * @param vadlFile the specification file
   * @return true if success, otherwise false
   */
  boolean runSpecification(URI vadlFile);

  /**
   * Get the VIAM from the run result. This must be called after
   * {@link TestFrontend#runSpecification}.
   */
  Specification getViam();

  /**
   * Get the logs that were emitted during execution as String.
   */
  String getLogAsString();

  /**
   * Holds the global frontend provider that can be dynamically set by the test executor.
   */
  abstract class Provider {

    /**
     * The global frontend provider.
     *
     * <p>In order to run the tests defined here, the {@code globalProvider} must be set
     * before running the tests. This is currently done in the old vadl project.</p>
     */
    @Nullable
    public static Provider globalProvider = new OpenVadlTestFrontend.Provider();

    /**
     * Creates a new instance of the {@link TestFrontend}.
     */
    public abstract TestFrontend createFrontend();
  }
}
