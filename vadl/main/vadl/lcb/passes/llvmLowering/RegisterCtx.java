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

package vadl.lcb.passes.llvmLowering;

import java.util.List;
import vadl.gcb.valuetypes.ValueType;
import vadl.viam.Definition;
import vadl.viam.DefinitionExtension;
import vadl.viam.RegisterTensor;

public class RegisterCtx extends DefinitionExtension<RegisterTensor> {
  private final List<ValueType> valueTypes;

  public RegisterCtx(List<ValueType> valueTypes) {
    this.valueTypes = valueTypes;
  }

  @Override
  public Class<? extends Definition> extendsDefClass() {
    return Definition.class;
  }

  public List<ValueType> valueTypes() {
    return valueTypes;
  }

  public Boolean used() {
    return !valueTypes.isEmpty();
  }
}
