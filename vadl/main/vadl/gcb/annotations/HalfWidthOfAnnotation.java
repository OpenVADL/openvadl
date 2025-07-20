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

package vadl.gcb.annotations;

import vadl.viam.Annotation;
import vadl.viam.ArtificialResource;
import vadl.viam.Resource;

/**
 * Annotation indicating the size of the alias which is crucial for
 * {@link CompilerRegisterRenamingAnnotation}.
 */
public class HalfWidthOfAnnotation extends Annotation<ArtificialResource> {
  private final int lo;
  private final int hi;
  private final Resource resource;

  /**
   * Constructor.
   */
  public HalfWidthOfAnnotation(int lo, int hi, Resource resource) {
    this.lo = lo;
    this.hi = hi;
    this.resource = resource;
  }

  @Override
  public Class<ArtificialResource> parentDefinitionClass() {
    return ArtificialResource.class;
  }
}
