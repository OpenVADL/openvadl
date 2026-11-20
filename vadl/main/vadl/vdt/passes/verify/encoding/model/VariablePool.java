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

package vadl.vdt.passes.verify.encoding.model;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Variable pool for the SAT encoding.
 */
public class VariablePool {
  private final AtomicInteger id = new AtomicInteger(1);
  private final Map<Object, Integer> variables = new HashMap<>();

  public int id(Object obj) {
    return variables.computeIfAbsent(obj, k -> id.getAndIncrement());
  }

  public int next() {
    return id.getAndIncrement();
  }

  public int size() {
    return id.get() - 1;
  }
}
