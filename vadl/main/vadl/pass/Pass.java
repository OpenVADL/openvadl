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

package vadl.pass;

import java.io.IOException;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.viam.Specification;

/**
 * A pass is a unit of execution. It analysis or transforms a VADL specification.
 */
public abstract class Pass {
  private GeneralConfiguration configuration;

  public Pass(GeneralConfiguration configuration) {
    this.configuration = configuration;
  }

  /**
   * Get the name of the pass.
   */
  public abstract PassName getName();

  /**
   * Execute the pass on the {@link Specification}.
   *
   * @param passResults are the results from the different passes which have been executed so far.
   * @param viam        is latest VADL specification. Note that transformation passes are allowed
   *                    to mutate the object.
   * @return the result of the pass. This will be automatically stored into {@code passResults} for
   *     the next pass by the {@link PassManager}.
   */
  @Nullable
  public abstract Object execute(final PassResults passResults, Specification viam)
      throws IOException;

  /**
   * This method is a hook which gets invoked after the {@link #execute(PassResults, Specification)}
   * has run. It can be used to verify that all required exists.
   *
   * @param viam       is latest VADL specification. Note that transformation passes are allowed
   *                   to mutate the object.
   * @param passResult is the result of this pass class and can be {@code null}.
   */
  public void verification(Specification viam, @Nullable Object passResult) {

  }

  public GeneralConfiguration configuration() {
    return configuration;
  }
}
