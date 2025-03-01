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

package vadl.viam.asm.rules;

import vadl.types.asmTypes.AsmType;
import vadl.viam.DefinitionVisitor;
import vadl.viam.Identifier;

/**
 * A TerminalRule is a simple rule referencing just a single token of the lexical analysis.
 */
public class AsmTerminalRule extends AsmGrammarRule {

  private final String value;

  public AsmTerminalRule(Identifier identifier, String value, AsmType asmType) {
    super(identifier, asmType);
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }
}
