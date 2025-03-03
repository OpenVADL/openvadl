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
import vadl.viam.Definition;
import vadl.viam.Identifier;
import vadl.viam.asm.elements.AsmGrammarElement;

/**
 * GrammarRules define the grammar of the assembly language,
 * from which the assembly parser is generated.
 */
public abstract class AsmGrammarRule extends Definition implements AsmGrammarElement {

  private final AsmType asmType;

  public AsmGrammarRule(Identifier identifier, AsmType asmType) {
    super(identifier);
    this.asmType = asmType;
  }

  public AsmType getAsmType() {
    return asmType;
  }
}