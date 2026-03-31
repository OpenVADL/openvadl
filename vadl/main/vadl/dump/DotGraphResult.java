// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.dump;

import java.util.Objects;
import vadl.pass.Pass;
import vadl.pass.PassKey;
import vadl.pass.PassResults;
import vadl.viam.Definition;

/**
 * Pass result wrapper for dot graphs shown in dumps.
 */
public class DotGraphResult extends PassResults.SingleResult implements BehaviorTimelineDisplay {
  private final Definition definition;

  public DotGraphResult(PassKey passKey,
                        Pass pass,
                        long durationMs,
                        String result,
                        boolean skipped,
                        Definition definition) {
    super(passKey, pass, durationMs, result, skipped);
    this.definition = definition;
  }

  @Override
  public String passId() {
    return passKey().value();
  }

  @Override
  public String passName() {
    return pass().getClass().getSimpleName();
  }

  @Override
  public String dotGraph() {
    return (String) Objects.requireNonNull(result());
  }

  public Definition definition() {
    return definition;
  }
}
