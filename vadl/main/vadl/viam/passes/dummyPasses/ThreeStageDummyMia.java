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
import vadl.viam.Logic;
import vadl.viam.Memory;
import vadl.viam.MicroArchitecture;
import vadl.viam.RegisterTensor;
import vadl.viam.Specification;
import vadl.viam.Stage;
import vadl.viam.StageOutput;
import vadl.viam.graph.Graph;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.MiaBuiltInCall;
import vadl.viam.graph.dependency.ReadStageOutputNode;
import vadl.viam.graph.dependency.WriteStageOutputNode;

/**
 * Three stage dummy MiA description.
 *
 * <p>This models a pipeline with stages Instruction Fetch, Instruction Decode and Execute following
 * the design of the Wildcat educational processor.
 *
 * <p>Schoeberl, M. (2025). Wildcat: Educational RISC-V Microprocessors. arXiv.
 * <a href="http://arxiv.org/abs/2502.20197">Paper</a>.
 */
class ThreeStageDummyMia {

  public static MicroArchitecture mia(Specification viam, InstructionSetArchitecture isa) {
    var regFile = viam.isa().orElseThrow().registerTensors()
        .stream().filter(RegisterTensor::isRegisterFile).findFirst().get();
    var mem = viam.isa().orElseThrow().ownMemories().get(0);

    var ident = Identifier.noLocation("MiA");
    var bypass = bypass(ident);
    var predict = predict(ident);

    var ifStage = ifStage(ident.append("IF"));
    var id = id(ident.append("ID"), ifStage.outputs().get(0), regFile, bypass);
    var ex = ex(ident.append("EX"), id.outputs().get(0), regFile, mem, bypass);

    return new MicroArchitecture(
        ident,
        isa,
        new ArrayList<>(List.of(ifStage, id, ex)),
        new ArrayList<>(List.of(bypass, predict))
    );
  }

  private static StageOutput stageOutput(Identifier ident, Type type) {
    return new StageOutput(ident, type);
  }

  /**
   * <pre>
   * [forwarding]
   * logic bypass
   * </pre>.
   */
  private static Logic.Forwarding bypass(Identifier parent) {
    var id = parent.append("bypass");
    return new Logic.Forwarding(id);
  }

  /**
   * <pre>
   * [branch predictor]
   * logic predict
   * </pre>.
   */
  private static Logic.BranchPrediction predict(Identifier parent) {
    var id = parent.append("predict");
    return new Logic.BranchPrediction(id);
  }

  /**
   * <pre>
   * stage IF -> ( fr : FetchResult ) =
   * {
   *   fr := fetchNext
   * }
   * </pre>.
   */
  private static Stage ifStage(Identifier ident) {
    var fr = stageOutput(ident.append("fr"), MicroArchitectureType.fetchResult());
    return new Stage(ident, ifBehavior(fr), List.of(fr));
  }

  private static Graph ifBehavior(StageOutput fr) {
    var beh = new Graph("IF");
    var fn = new MiaBuiltInCall(BuiltInTable.FETCH_NEXT, new NodeList<>(),
        MicroArchitectureType.fetchResult());
    var wr = new WriteStageOutputNode(fr, fn);
    beh.addWithInputs(wr);
    return beh;
  }

  /**
   * <pre>
   * stage ID -> ( ir : Instruction ) =
   * {
   *   let instr = decode( IF.fr ) in
   *   {
   *     instr.readOrForward( @X, @bypass )
   *     ir := instr
   *   }
   * }
   * </pre>.
   */
  private static Stage id(Identifier ident, StageOutput ifFr, RegisterTensor regFile,
                          Logic bypass) {
    var ir = stageOutput(ident.append("ir"), MicroArchitectureType.instruction());
    return new Stage(ident, idBehavior(ifFr, ir, regFile, bypass), List.of(ir));
  }

  private static Graph idBehavior(StageOutput ifFr, StageOutput ir, RegisterTensor regFile,
                                  Logic bypass) {
    var rd = new ReadStageOutputNode(ifFr);
    var i1 = new MiaBuiltInCall(BuiltInTable.DECODE, new NodeList<>(rd),
        MicroArchitectureType.instruction());
    var i2 = new MiaBuiltInCall(BuiltInTable.INSTRUCTION_READ_OR_FORWARD, new NodeList<>(i1),
        MicroArchitectureType.instruction());
    i2.add(regFile);
    i2.add(bypass);
    var wr = new WriteStageOutputNode(ir, i2);
    var beh = new Graph("ID");
    beh.addWithInputs(wr);
    return beh;
  }

  /**
   * <pre>
   * stage EX -> ( ir : Instruction ) =
   * {
   *   let instr = ID.ir in
   *   {
   *     instr.results( @X, @bypass )
   *     instr.read( @MEM )
   *     instr.write
   *     ir := instr
   *   }
   * }
   * </pre>.
   */
  private static Stage ex(Identifier ident, StageOutput idIr, RegisterTensor regFile, Memory mem,
                          Logic bypass) {
    var ir = stageOutput(ident.append("ir"), MicroArchitectureType.instruction());
    return new Stage(ident, exBehavior(idIr, ir, regFile, mem, bypass), List.of(ir));
  }

  private static Graph exBehavior(StageOutput idIr, StageOutput ir, RegisterTensor regFile,
                                  Memory mem, Logic bypass) {
    var rd = new ReadStageOutputNode(idIr);
    var i1 = new MiaBuiltInCall(BuiltInTable.INSTRUCTION_RESULTS, new NodeList<>(rd),
        MicroArchitectureType.instruction());
    i1.add(regFile);
    i1.add(bypass);
    var i2 = new MiaBuiltInCall(BuiltInTable.INSTRUCTION_READ, new NodeList<>(i1),
        MicroArchitectureType.instruction());
    i2.add(mem);
    var i3 = new MiaBuiltInCall(BuiltInTable.INSTRUCTION_WRITE, new NodeList<>(i2),
        MicroArchitectureType.instruction());
    var wr = new WriteStageOutputNode(ir, i3);
    var beh = new Graph("EX");
    beh.addWithInputs(wr);
    return beh;
  }

}
