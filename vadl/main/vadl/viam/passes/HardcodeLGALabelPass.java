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

package vadl.viam.passes;

import java.io.IOException;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.Type;
import vadl.viam.Specification;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.graph.control.NewLabelNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.FuncCallNode;
import vadl.viam.graph.dependency.LabelNode;

/**
 * Temporary pass for inserting labels into a pseudo instruction.
 * See {@code https://github.com/OpenVADL/openvadl/issues/148}.
 */
public class HardcodeLGALabelPass extends Pass {
  public HardcodeLGALabelPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("HardcodeLGALabelPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    viam.isa().ifPresent(isa -> isa
        .ownPseudoInstructions()
        .stream().filter(instruction -> instruction.simpleName().equals("LGA_64"))
        .forEach(instruction -> {
          var labelNode = new LabelNode(Type.signedInt(32));
          var startNode =
              instruction.behavior().getNodes(StartNode.class).findFirst().orElseThrow();
          var newlabelNode = new NewLabelNode(labelNode);
          startNode.addAfter(newlabelNode);

          var ldInstruction = instruction.behavior().getNodes(InstrCallNode.class)
              .filter(x -> x.target().simpleName().equals("LD"))
              .findFirst()
              .orElseThrow();

          var pcrel = (FuncCallNode) ldInstruction.arguments().get(2);
          pcrel.arguments().get(0).replaceAndDelete(labelNode);
        }));

    return null;
  }
}
