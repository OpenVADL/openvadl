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

package vadl.viam.passes.dummyPasses;

import java.util.ArrayList;
import java.util.List;
import vadl.types.BuiltInTable;
import vadl.types.MicroArchitectureType;
import vadl.types.Type;
import vadl.viam.Identifier;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.MicroArchitecture;
import vadl.viam.Stage;
import vadl.viam.StageOutput;
import vadl.viam.graph.Graph;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.MiaBuiltInCall;
import vadl.viam.graph.dependency.WriteStageOutputNode;

/**
 * Single stage dummy MiA description.
 *
 * <p>Placing the whole instruction behavior in a single pipeline stage may require the stage to
 * take more than one cycle for execution. In this case an implementation could share resources
 * between the execution steps (which our hardware generation currently does not do).
 */
class SingleStageDummyMia {

  public static MicroArchitecture mia(InstructionSetArchitecture isa) {
    var ident = Identifier.noLocation("MiA");

    var issStage = iss(ident.append("ISS"));

    return new MicroArchitecture(
        ident,
        isa,
        new ArrayList<>(List.of(issStage)),
        new ArrayList<>()
    );
  }

  private static StageOutput stageOutput(Identifier ident, Type type) {
    return new StageOutput(ident, type);
  }

  /**
   * <pre>
   * stage ISS -> ( ir : Instruction ) =
   * {
   *   let instr = decode ( fetchNext ) in
   *   {
   *     instr.write
   *     ir := instr
   *   }
   * }
   * </pre>.
   */
  private static Stage iss(Identifier ident) {
    var ir = stageOutput(ident.append("fr"), MicroArchitectureType.instruction());
    return new Stage(ident, issBehavior(ir), List.of(ir));
  }

  private static Graph issBehavior(StageOutput ir) {
    var beh = new Graph("ISS");
    var fn = new MiaBuiltInCall(BuiltInTable.FETCH_NEXT, new NodeList<>(),
        MicroArchitectureType.fetchResult());
    var i1 = new MiaBuiltInCall(BuiltInTable.DECODE, new NodeList<>(fn),
        MicroArchitectureType.instruction());
    var i2 = new MiaBuiltInCall(BuiltInTable.INSTRUCTION_WRITE, new NodeList<>(i1),
        MicroArchitectureType.instruction());
    var wr = new WriteStageOutputNode(ir, i2);
    beh.addWithInputs(wr);
    return beh;
  }

}
