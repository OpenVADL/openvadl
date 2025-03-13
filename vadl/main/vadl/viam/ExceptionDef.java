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

import vadl.viam.graph.Graph;

/**
 * Represents a VADL exception.
 * It is essentially a {@link Procedure} without arguments.
 * There are two different {@link Kind}s of exceptions: declared and anonymous ones.
 *
 * <p>It has the Def suffix to avoid a name clash with {@code java.lang.Exception}.</p>
 */
public class ExceptionDef extends Procedure {

  /**
   * Defines if the exception was declared like {@code exception XY = { ... }}
   * or was implicitly constructed using {@code raise { ... }}.
   */
  public enum Kind {
    DECLARED,
    ANONYMOUS,
  }

  private final Kind kind;

  /**
   * Constructs the exception.
   */
  public ExceptionDef(Identifier identifier, Graph behavior, Kind kind) {
    super(identifier, new Parameter[] {}, behavior);
    this.kind = kind;
  }

  public Kind kind() {
    return kind;
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }
}
