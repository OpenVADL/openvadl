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

/**
 * Represents an {@link Identifier} or one of the placeholder sub types.
 * A placeholder is some macro expr that cannot be directly expanded to an {@link Identifier}, as
 * itself is part of a macro definition.
 *
 * <p>For instance in the following {@code $constId} is such a placeholder:
 * <pre>{@code
 * model Test (constId: Id) : Defs = {
 *   constant $constId = 4
 * }
 * }</pre></p>
 */
@SuppressWarnings("MissingJavadocMethod")
public sealed interface IdentifierOrPlaceholder extends IsId
    permits Identifier, MacroInstanceExpr, MacroMatchExpr, PlaceholderExpr, AsIdExpr {
}
