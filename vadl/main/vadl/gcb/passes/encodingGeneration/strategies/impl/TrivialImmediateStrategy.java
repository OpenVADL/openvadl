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

package vadl.gcb.passes.encodingGeneration.strategies.impl;

import vadl.gcb.passes.encodingGeneration.strategies.EncodingGenerationStrategy;
import vadl.viam.Constant;
import vadl.viam.Format;
import vadl.viam.PrintableInstruction;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.SliceNode;

/**
 * A trivial immediate is where the field access function is simply an immediate.
 * <pre>{@code
 * format Utype : Inst =
 * {     imm    : Bits<20>
 * , rd     : Index
 * , opcode : Bits7
 * , immU = imm as UInt<32>
 * }
 * }</pre>
 * This class should compute the following encoding function automatically:
 * <pre>{@code
 * encode {
 * imm => immU(19..0)
 * }
 * }</pre>
 */
public class TrivialImmediateStrategy implements EncodingGenerationStrategy {
  @Override
  public boolean checkIfApplicable(Format.FieldAccess fieldAccess) {
    // Checks whether the behavior does not contain any {@link BuiltIn} or {@link SliceNode}.
    var behavior = fieldAccess.accessFunction().behavior();
    return behavior.getNodes()
        .filter(x -> x instanceof BuiltInCall || x instanceof SliceNode)
        .findAny().isEmpty();
  }

  @Override
  public void generateEncoding(PrintableInstruction instruction, Format.FieldAccess fieldAccess) {
    var fieldRef = fieldAccess.fieldRef();
    // The field takes up a certain slice.
    // But we need to take a slice of the immediate of the same size.
    var fieldAccessBitSlice = fieldRef.bitSlice();
    var invertedSlice = new Constant.BitSlice(
        Constant.BitSlice.Part.of(fieldAccessBitSlice.bitSize() - 1, 0));
    var invertedSliceNode = new SliceNode(new FieldAccessRefNode(fieldAccess, fieldAccess.type()),
        invertedSlice,
        fieldRef.type());
    var returnNode = new ReturnNode(invertedSliceNode);
    var startNode = new StartNode(returnNode);

    var behavior = new Graph("Generated encoding of " + fieldAccess.simpleName());
    behavior.addWithInputs(returnNode);
    behavior.add(startNode);
    setFieldEncoding(instruction, fieldAccess, behavior);
  }
}
