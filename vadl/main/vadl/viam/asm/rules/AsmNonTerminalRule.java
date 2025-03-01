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
import vadl.utils.SourceLocation;
import vadl.viam.DefinitionVisitor;
import vadl.viam.Identifier;
import vadl.viam.asm.elements.AsmAlternatives;

/**
 * A NonTerminalRule is a complex rule composed of references to other rules,
 * calls to functions, literals and block elements like Group, Option, Repetition.
 */
public class AsmNonTerminalRule extends AsmGrammarRule {

  private final AsmAlternatives alternatives;

  /**
   * Creates a new NonTerminalRule.
   *
   * @param identifier   the identifier of the rule
   * @param alternatives the alternatives of the rule
   * @param location     the source location of the rule
   */
  public AsmNonTerminalRule(Identifier identifier, AsmAlternatives alternatives,
                            AsmType asmType, SourceLocation location) {
    super(identifier, asmType);
    this.alternatives = alternatives;
    this.setSourceLocation(location);
  }

  public AsmAlternatives getAlternatives() {
    return alternatives;
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }
}
