// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.configuration;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.stream.Streams;

/**
 * Defines the dump modes for the generator.
 */
public enum DumpMode {
  /**
   * Dumps are always generated.
   */
  ALWAYS,
  /**
   * Dumps are only generated if an internal error occurs.
   */
  ON_FAILURE,
  /**
   * No dumps are generated.
   */
  NONE;

  public static final Set<String> modeStrings = Streams.of(values())
      .filter(mode -> mode != NONE)
      .map(DumpMode::toString)
      .collect(Collectors.toCollection(LinkedHashSet::new));

  @Override
  public String toString() {
    return name().toLowerCase().replace('_', '-');
  }

  public static DumpMode fromString(String value) {
    return valueOf(value.toUpperCase().replace('-', '_'));
  }
}
