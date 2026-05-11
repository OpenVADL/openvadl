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

package vadl.viam.passes.pcOffset;

import java.io.IOException;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BuiltInTable;
import vadl.utils.GraphUtils;
import vadl.utils.ViamUtils;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.annotations.PcOffsetAnnotation;
import vadl.viam.graph.Graph;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ReadResourceNode;

/**
 * Applies program counter offsets to program counter reads.
 *
 * <p>When reading a program counter that has been annotated with
 * {@code [current]}, {@code [next]} or {@code [next next]} or when using
 * one of the subcalls {@code .current}, {@code .next} or {@code .nextnext},
 * the read value is offset by multiples of the instruction length. The subcalls
 * overwrite the annotations.
 *
 * <p>This pass looks for {@link PcOffsetAnnotation}s and checks
 * {@link ReadResourceNode#pcOffset()} and applies them by inserting addition nodes.
 */
public class PcOffsetPass extends Pass {

  public PcOffsetPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("PC Offset Pass");
  }

  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam)
      throws IOException {
    ViamUtils.findAllBehaviors(viam).forEach(this::handleBehaviour);
    return null;
  }

  private void handleBehaviour(Graph behaviour) {
    // FIXME: are instruction lengths always multiples of 8?
    //        What if the pc does not counter per byte?
    // FIXME: What instruction length should be used in behaviours outside
    //        instructions (eg in functions)?
    int instrBytes = behaviour.parentDefinition() instanceof Instruction instruction
        ? instruction.format().type().bitWidth() / 8
        : 32;

    behaviour.getNodes(ReadResourceNode.class)
        .forEach(n -> handleRead(n, instrBytes));
  }

  private void handleRead(ReadResourceNode read, int instrBytes) {
    var offsetAnn = read.resourceDefinition().annotation(PcOffsetAnnotation.class);
    var regOffset = offsetAnn == null ? 0 : offsetAnn.offset();
    var readOffset = read.pcOffset();
    int offset = readOffset != null ? readOffset : regOffset;

    if (offset != 0) {
      read.replace(BuiltInCall.of(
          BuiltInTable.ADD, read,
          GraphUtils.intUNode((long) offset * instrBytes, read.type().bitWidth())
      ));
    }
  }
}
