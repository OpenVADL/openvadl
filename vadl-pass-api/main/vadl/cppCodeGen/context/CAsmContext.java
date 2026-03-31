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

package vadl.cppCodeGen.context;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import vadl.viam.asm.elements.AsmGrammarElement;

/**
 * A code generation context for {@link AsmGrammarElement}s.
 * Used in the assembler generator {@link vadl.lcb.codegen.assembly.AssemblyParserCodeGenerator}.
 */
public class CAsmContext extends CGenContext<AsmGrammarElement> {

  protected BiConsumer<CAsmContext, AsmGrammarElement> dispatch;

  public CAsmContext(Consumer<String> writer, BiConsumer<CAsmContext, AsmGrammarElement> dispatch) {
    super(writer, "");
    this.dispatch = dispatch;
  }

  @Override
  public CGenContext<AsmGrammarElement> spacedIn() {
    prefix += "  ";
    return this;
  }

  @Override
  public CGenContext<AsmGrammarElement> spaceOut() {
    if (prefix.length() > 2) {
      prefix = prefix.substring(2);
    }
    return this;
  }

  @Override
  public CGenContext<AsmGrammarElement> gen(AsmGrammarElement entity) {
    dispatch.accept(this, entity);
    return this;
  }

  @Override
  public String genToString(AsmGrammarElement entity) {
    var builder = new StringBuilder();
    var subContext = new CAsmContext(builder::append, dispatch);
    subContext.gen(entity);
    return builder.toString();
  }
}
