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

import vadl.types.Type;

/**
 * Stage output definition in MiA description.
 *
 * <p>A stage output belongs to a stage is written by WriteStageOutputNodes (part of the
 * stage's behavior) and read by ReadStageOutputNodes (also in other stage's behaviors).
 */
public class StageOutput extends Definition implements DefProp.WithType {

  private final Type type;

  public StageOutput(Identifier identifier, Type type) {
    super(identifier);
    this.type = type;
  }

  @Override
  public Type type() {
    return type;
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    return identifier.simpleName() + ": " + type;
  }
}
