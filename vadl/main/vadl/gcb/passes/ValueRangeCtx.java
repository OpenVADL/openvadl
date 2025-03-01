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

package vadl.gcb.passes;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import vadl.viam.Definition;
import vadl.viam.DefinitionExtension;
import vadl.viam.Format;
import vadl.viam.Instruction;

/**
 * An extension for the {@link Instruction}. It will be used to indicate what value
 * ranges the instruction's immediates have.
 */
public class ValueRangeCtx extends DefinitionExtension<Instruction> {
  private final IdentityHashMap<Format.Field, ValueRange> ranges;

  public ValueRangeCtx() {
    this.ranges = new IdentityHashMap<>();
  }

  public Map<Format.Field, ValueRange> ranges() {
    return ranges;
  }

  /**
   * Add a new range.
   */
  public void add(Format.Field field, ValueRange range) {
    ranges.put(field, range);
  }

  /**
   * Get the first range.
   */
  public Optional<ValueRange> getFirst() {
    return this.ranges.values().stream().findFirst();
  }

  @Override
  public Class<? extends Definition> extendsDefClass() {
    return Definition.class;
  }
}
