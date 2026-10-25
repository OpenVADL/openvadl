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

package vadl.types.miaTypes;

import vadl.types.Type;

/**
 * The type of {@link vadl.viam.Stage}. This is parameterized with the stage's
 * output type, which can be retrieved using {@link #consumedType()}.
 */
public class StageType extends ConsumableType {

  private final Type consumedType;

  protected StageType(Type consumedType) {
    this.consumedType = consumedType;
  }

  @Override
  public String name() {
    return "Stage<%s>".formatted(consumedType.name());
  }

  @Override
  public Type consumedType() {
    return consumedType;
  }
}
