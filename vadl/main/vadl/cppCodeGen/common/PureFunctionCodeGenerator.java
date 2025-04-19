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

package vadl.cppCodeGen.common;

import static vadl.error.DiagUtils.throwNotAllowed;

import vadl.cppCodeGen.FunctionCodeGenerator;
import vadl.cppCodeGen.context.CGenContext;
import vadl.viam.Function;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.NewLabelNode;
import vadl.viam.graph.dependency.AsmBuiltInCall;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.ReadArtificialResNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.ReadRegNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.SliceNode;

/**
 * Produce a pure function that does not access any entities except parameters.
 */
public class PureFunctionCodeGenerator extends FunctionCodeGenerator {

  /**
   * Creates a new pure function code generator for the specified function.
   *
   * @param function the function for which code should be generated
   */
  public PureFunctionCodeGenerator(Function function) {
    super(function);
  }

  @Override
  protected void handle(CGenContext<Node> ctx, ReadRegNode toHandle) {
    throwNotAllowed(toHandle, "Register reads");
  }

  @Override
  protected void handle(CGenContext<Node> ctx, ReadRegFileNode toHandle) {
    throwNotAllowed(toHandle, "Register reads");
  }

  @Override
  protected void handle(CGenContext<Node> ctx, ReadRegTensorNode toHandle) {
    throwNotAllowed(toHandle, "Register reads");
  }

  @Override
  protected void handle(CGenContext<Node> ctx, ReadMemNode toHandle) {
    throwNotAllowed(toHandle, "Memory reads");
  }

  @Override
  protected void handle(CGenContext<Node> ctx, ReadArtificialResNode toHandle) {
    throwNotAllowed(toHandle, "Artificial resource reads");
  }

  @Override
  protected void handle(CGenContext<Node> ctx, FieldAccessRefNode toHandle) {
    throwNotAllowed(toHandle, "Format field accesses");
  }

  @Override
  public void handle(CGenContext<Node> ctx, FieldRefNode toHandle) {
    throwNotAllowed(toHandle, "Format field accesses");
  }

  @Override
  public void handle(CGenContext<Node> ctx, AsmBuiltInCall toHandle) {
    throwNotAllowed(toHandle, "Asm builtin calls");
  }

  @Override
  public void handle(CGenContext<Node> ctx, SliceNode toHandle) {
    throwNotAllowed(toHandle, "Slice node reads");
  }
}
