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

package vadl.cppCodeGen.context;

import static vadl.viam.ViamError.ensureNonNull;

import java.util.HashMap;
import java.util.Map;
import vadl.viam.graph.Node;

/**
 * A code generation context for {@link Node}s.
 * It extends the {@link CNodeContext} with additional information.
 * This is required when the handle methods do not have enough parameters.
 */
public class CNodeWithBaggageContext extends CNodeContext {

  private final Map<String, Object> baggage = new HashMap<>();

  /**
   * Construct a new code generation context for {@link Node}s.
   */
  public CNodeWithBaggageContext(CNodeContext context) {
    super(context.writer, context.prefix, context.dispatch);
  }

  /**
   * Get a string from the context.
   */
  public String getString(String key) {
    return get(key, String.class);
  }

  /**
   * Get an object from the context and cast it to {@code clazz}.
   */
  public <T> T get(String key, Class<T> clazz) {
    return ensureNonNull(clazz.cast(baggage.get(key)), "must not be null");
  }

  /**
   * Store a value into the context.
   */
  public CNodeWithBaggageContext put(String key, Object value) {
    baggage.put(key, value);
    return this;
  }
}
