// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.lcb.passes.asm;

import com.google.errorprone.annotations.concurrent.LazyInit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import vadl.viam.PrintableInstruction;
import vadl.viam.asm.AsmToken;
import vadl.viam.asm.elements.AsmGrammarElement;
import vadl.viam.asm.rules.AsmGrammarRule;

/**
 * A context for building an assembly grammar rule from an
 * instruction's assembly printing function.
 */
public class AsmRuleContext {
  PrintableInstruction instruction;

  // TODO: Use a stack to allow nested alternatives
  List<AsmGrammarElement> currentElements = new ArrayList<>();

  @LazyInit
  Set<AsmToken> firstTokens;

  @LazyInit
  AsmGrammarRule builtRule;

  public AsmRuleContext(PrintableInstruction instruction) {
    this.instruction = instruction;
  }

  public void addElement(AsmGrammarElement element) {
    currentElements.add(element);
  }

  /**
   * Set the firstTokens if the field is null.
   *
   * @param firstTokens the token to set
   */
  public void setFirstTokensIfNull(Set<AsmToken> firstTokens) {
    if (this.firstTokens == null) {
      this.firstTokens = firstTokens;
    }
  }
}
