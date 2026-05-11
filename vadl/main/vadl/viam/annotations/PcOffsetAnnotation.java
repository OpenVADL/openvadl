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

package vadl.viam.annotations;

import vadl.viam.Annotation;
import vadl.viam.RegisterTensor;

/**
 * Annotation for offsetting read pc values by one or two instruction
 * lengths. Can be overwritten by {@link vadl.viam.passes.pcOffset.nodes.PcOffsetNode}.
 *
 * <p>The offset is applied by {@link vadl.viam.passes.pcOffset.PcOffsetPass}.
 */
public class PcOffsetAnnotation extends Annotation<RegisterTensor> {

  public PcOffsetAnnotation(int offset) {
    this.offset = offset;
  }

  private final int offset;

  @Override
  public Class<RegisterTensor> parentDefinitionClass() {
    return RegisterTensor.class;
  }

  /**
   * The offset in instructions lengths that is added.
   */
  public int offset() {
    return offset;
  }
}
