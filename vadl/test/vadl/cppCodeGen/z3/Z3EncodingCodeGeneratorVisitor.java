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

package vadl.cppCodeGen.z3;

import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.passes.translation_validation.Z3CodeGeneratorVisitor;

public class Z3EncodingCodeGeneratorVisitor extends Z3CodeGeneratorVisitor {

  private final String symbolName;

  // from z3 import *
  // x = BitVec('x', 20) # field
  // f_x = ZeroExt(12, x)
  // f_z = Extract(19, 0, f_x)
  // prove (x == f_z)
  //
  // The trick is that f_z references f_x and
  // does all the inverse operations.
  // However, we want to apply for both functions
  // the same visitor.
  // That's why we have 'symbolName' in the constructor.
  // In the case of 'f_x' this is the field
  // In the case of 'f_z' this is the function parameter
  public Z3EncodingCodeGeneratorVisitor(String symbolName) {
    super();
    this.symbolName = symbolName;
  }

  @Override
  public void visit(FuncParamNode funcParamNode) {
    writer.write(symbolName);
  }


  @Override
  public void visit(FieldRefNode fieldRefNode) {
    writer.write(symbolName);
  }
}
