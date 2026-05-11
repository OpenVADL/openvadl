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

package vadl.viam;

import java.util.List;
import vadl.types.DataType;
import vadl.utils.SourceLocation;
import vadl.utils.WithLocation;

/**
 * Refers to an architectural register resource at fixed indices.
 */
public record RegisterRef(
    RegisterResource resource,
    List<Constant.Value> indices,
    SourceLocation location
) implements WithLocation {

  /**
   * Constructs a register reference and verifies that the given fixed indices fit the resource.
   */
  public RegisterRef {
    ensure(indices.size() <= resource.indexTypes().size(),
        "Too many register indices for %s: expected at most %s, got %s",
        resource.simpleName(), resource.indexTypes().size(), indices.size());
  }

  public DataType resultType() {
    return resource.resultType(indices.size());
  }

  /**
   * Returns the only index as integer.
   *
   * <p>This is a compatibility helper for existing one-dimensional register-file users.</p>
   */
  public int singleIndex() {
    ensure(indices.size() == 1, "Expected exactly one register index, got %s", indices.size());
    return indices.getFirst().intValue();
  }
}
