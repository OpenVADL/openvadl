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

package vadl.utils.codegen;

/**
 * An interface for appending code to a buffer, handling indentation and new lines gracefully.
 */
public interface CodeGeneratorAppendable {

  CodeGeneratorAppendable append(CharSequence csq);

  CodeGeneratorAppendable append(Object obj);

  CodeGeneratorAppendable appendLn(CharSequence csq);

  CodeGeneratorAppendable appendLn(Object obj);

  CodeGeneratorAppendable newLine();

  CodeGeneratorAppendable indent();

  CodeGeneratorAppendable unindent();

  CharSequence toCharSequence();
}
