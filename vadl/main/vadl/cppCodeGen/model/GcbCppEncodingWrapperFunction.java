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

package vadl.cppCodeGen.model;

import java.util.List;
import java.util.Set;
import vadl.types.Type;
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.Identifier;
import vadl.viam.Instruction;
import vadl.viam.Parameter;
import vadl.viam.graph.Graph;

public class GcbCppEncodingWrapperFunction extends Function {

  private final Instruction instruction;
  private final Set<Format.FieldAccess> fieldAccesses;
  private final List<Format.FieldEncoding> encodings;
  private final List<GcbCppEncodeFunction> encodingFunctions;

  public GcbCppEncodingWrapperFunction(Identifier identifier,
                                       Parameter[] parameters,
                                       Type returnType,
                                       Instruction instruction,
                                       Set<Format.FieldAccess> fieldAccesses,
                                       List<Format.FieldEncoding> encodings,
                                       List<GcbCppEncodeFunction> encodingFunctions) {
    super(identifier, parameters, returnType, new Graph("empty graph"));

    this.instruction = instruction;
    this.fieldAccesses = fieldAccesses;
    this.encodings = encodings;
    this.encodingFunctions = encodingFunctions;
  }

  public List<Format.FieldEncoding> encodings() {
    return encodings;
  }

  public Set<Format.FieldAccess> fieldAccesses() {
    return fieldAccesses;
  }

  public Instruction instruction() {
    return instruction;
  }

  public List<GcbCppEncodeFunction> encodingFunctions() {
    return encodingFunctions;
  }
}
