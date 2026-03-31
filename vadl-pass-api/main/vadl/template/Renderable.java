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

package vadl.template;

import java.util.Map;

/**
 * Marks classes that can be rendered by the template {@link AbstractTemplateRenderingPass}.
 * To produce native images that do not crash at runtime during rendering because of missing
 * reflection information, we reduce the number of allowed variable types to
 * {@code Map, List, String} and primitive types (+ their wrappers).
 * If generators use custom classes for rendering variables, they can implement this interface
 * and the {@link #renderObj()} method to enable it as variable type.
 */
public interface Renderable {

  /**
   * Renders the object into a valid map that can be rendered.
   * The values of the returned map can be one of the supported types and classes
   * that also implement {@link Renderable}.
   */
  Map<String, Object> renderObj();

}
