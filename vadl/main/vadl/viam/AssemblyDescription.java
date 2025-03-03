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

import java.util.List;
import vadl.viam.asm.AsmDirectiveMapping;
import vadl.viam.asm.AsmModifier;
import vadl.viam.asm.rules.AsmGrammarRule;

/**
 * An Assembly Description definition of a VADL specification.
 */
public class AssemblyDescription extends Definition {

  private final List<AsmModifier> modifiers;
  private final List<AsmDirectiveMapping> directives;
  private final List<AsmGrammarRule> rules;
  private final List<Definition> commonDefinitions;

  /**
   * Creates a new Assembly Description definition.
   *
   * @param identifier        the identifier of the definition
   * @param modifiers         the modifiers of the definition
   * @param directives        the directives of the definition
   * @param rules             the rules of the definition
   * @param commonDefinitions the common definitions of the definition
   */
  public AssemblyDescription(Identifier identifier, List<AsmModifier> modifiers,
                             List<AsmDirectiveMapping> directives, List<AsmGrammarRule> rules,
                             List<Definition> commonDefinitions) {
    super(identifier);
    this.modifiers = modifiers;
    this.directives = directives;
    this.rules = rules;
    this.commonDefinitions = commonDefinitions;
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }

  public List<AsmModifier> modifiers() {
    return modifiers;
  }

  public List<AsmDirectiveMapping> directives() {
    return directives;
  }

  public List<AsmGrammarRule> rules() {
    return rules;
  }

  public List<Definition> commonDefinitions() {
    return commonDefinitions;
  }
}
