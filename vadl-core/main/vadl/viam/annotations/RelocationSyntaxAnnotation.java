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

package vadl.viam.annotations;

import vadl.viam.Annotation;
import vadl.viam.Relocation;

/**
 * Defines how the compiler generator will print the relocation in the assembly.
 */
public class RelocationSyntaxAnnotation extends Annotation<Relocation> {
  private final String formatString;

  public RelocationSyntaxAnnotation(String formatString) {
    this.formatString = formatString;
  }

  @Override
  public Class<Relocation> parentDefinitionClass() {
    return Relocation.class;
  }

  public String formatString() {
    return formatString;
  }
}
