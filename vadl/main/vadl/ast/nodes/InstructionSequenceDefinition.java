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

package vadl.ast.nodes;

import java.util.List;
import vadl.javaannotations.ast.Child;
import vadl.utils.SourceLocation;

@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public abstract class InstructionSequenceDefinition extends Definition {
  @Child
  public List<Parameter> params;
  @Child
  public List<InstructionCallStatement> statements;
  public SourceLocation loc;

  public InstructionSequenceDefinition(List<Parameter> params,
                                       List<InstructionCallStatement> statements,
                                       SourceLocation loc) {
    this.params = params;
    this.statements = statements;
    this.loc = loc;
  }

  @Override
  public SourceLocation location() {
    return loc;
  }
}
