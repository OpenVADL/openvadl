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

@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public interface StatementVisitor<T> {
  public T visit(AssignmentStatement statement);

  public T visit(BlockStatement statement);

  public T visit(CallStatement statement);

  public T visit(ForallStatement statement);

  public T visit(IfStatement statement);

  public T visit(InstructionCallStatement statement);

  public T visit(LetStatement statement);

  public T visit(LockStatement statement);

  public T visit(MacroInstanceStatement statement);

  public T visit(MacroMatchStatement statement);

  public T visit(MatchStatement statement);

  public T visit(PlaceholderStatement statement);

  public T visit(RaiseStatement statement);

  public T visit(StatementList statement);
}
