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

package vadl.viam.annotations;

import java.util.List;
import vadl.viam.Annotation;
import vadl.viam.Assembly;
import vadl.viam.Format;

/**
 * AsmEncodeAnno is a subclass of Annotation that provides the functionality to associate
 * a list of fields to encode with an Assembly definition.
 *
 * <p>Example:<pre>{@code
 *  [encode imm20 ]
 *  assembly LUI = (mnemonic, ' ', register( rd ), ',', hex( imm20 ))
 * }</pre></p>
 */
public class AsmEncodeAnno extends Annotation<Assembly> {

  private final List<Format.FieldAccess> fieldsToEncode;

  public AsmEncodeAnno(List<Format.FieldAccess> fieldsToEncode) {
    this.fieldsToEncode = fieldsToEncode;
  }

  public List<Format.FieldAccess> fieldsToEncode() {
    return fieldsToEncode;
  }

  @Override
  public Class<Assembly> parentDefinitionClass() {
    return Assembly.class;
  }
}
