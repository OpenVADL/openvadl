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

package vadl.viam;

import javax.annotation.Nullable;

/**
 * Definition extensions allow passes to attach information to specific definitions.
 * Later on, this information can be directly accessed from a given VIAM definition.
 */
public abstract class DefinitionExtension<T extends Definition> {

  // set automatically when added to definition
  @Nullable
  private T extendingDefinition;

  public DefinitionExtension() {
  }

  /**
   * Returns the class of definition this extension extends.
   */
  public abstract Class<? extends Definition> extendsDefClass();

  /**
   * Returns the definition extended by this extension.
   * This must be called AFTER the extension was added to the definition.
   */
  public T extendingDef() {
    if (extendingDefinition == null) {
      throw new IllegalStateException("Extension not yet added to definition.");
    }
    return extendingDefinition;
  }

  /**
   * Sets the definition extended by this. This is only called by the
   * {@link Definition#attachExtension(DefinitionExtension)}.
   */
  protected void setExtendingDefinition(T def) {
    if (def == null) {
      throw new IllegalArgumentException("Extension already added to definition.");
    }
    extendingDefinition = def;
  }

}
