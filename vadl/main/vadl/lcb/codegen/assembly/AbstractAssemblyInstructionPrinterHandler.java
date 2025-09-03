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

package vadl.lcb.codegen.assembly;

import com.google.errorprone.annotations.concurrent.LazyInit;
import vadl.cppCodeGen.context.CGenContext;
import vadl.cppCodeGen.context.CNodeContext;
import vadl.cppCodeGen.mixins.CDefaultMixins;
import vadl.cppCodeGen.mixins.CInvalidMixins;
import vadl.error.Diagnostic;
import vadl.javaannotations.Handler;
import vadl.lcb.graph.DefinedImmediateSideEffectNode;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstruction;
import vadl.viam.PrintableInstruction;
import vadl.viam.graph.Node;

/**
 * Abstract class for assembly printing functionality.
 */
public abstract class AbstractAssemblyInstructionPrinterHandler
    implements CDefaultMixins.AllExpressions, CInvalidMixins {

  protected final PrintableInstruction instruction;
  protected final TableGenInstruction tableGenInstruction;

  @LazyInit
  protected CNodeContext ctx;
  protected final StringBuilder builder = new StringBuilder();

  /**
   * Constructor.
   */
  public AbstractAssemblyInstructionPrinterHandler(PrintableInstruction instruction,
                                                   TableGenInstruction tableGenInstruction) {
    this.instruction = instruction;
    this.tableGenInstruction = tableGenInstruction;
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(CGenContext<Node> ctx, DefinedImmediateSideEffectNode node) {
    throw Diagnostic.error(
        "not supported",
        node.location()).build();
  }
}
