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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BuiltInTable;
import vadl.types.MicroArchitectureType;
import vadl.types.Type;
import vadl.viam.Identifier;
import vadl.viam.Logic;
import vadl.viam.Memory;
import vadl.viam.MicroArchitecture;
import vadl.viam.RegisterFile;
import vadl.viam.Resource;
import vadl.viam.Specification;
import vadl.viam.Stage;
import vadl.viam.StageOutput;
import vadl.viam.graph.Graph;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.MiaBuiltInCall;
import vadl.viam.graph.dependency.ReadStageOutputNode;
import vadl.viam.graph.dependency.WriteStageOutputNode;

/**
 * Adds a hardcoded {@link vadl.viam.MicroArchitecture} definition to the VIAM specification.
 * This is deleted as soon as the frontend can handle the translation.
 */
public class DummyMiaPass extends Pass {

  public DummyMiaPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("Dummy Micro Architecture");
  }

  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam)
      throws IOException {

    if (viam.mia().isPresent()) {
      return null;
    }

    var mip = viam.mip().orElse(null);

    if (mip == null) {
      // if there is no mip, we just do nothing
      return null;
    }

    var regFile = viam.isa().orElseThrow().ownRegisterFiles().get(0);
    var mem = viam.isa().orElseThrow().ownMemories().get(0);
    var pc = Objects.requireNonNull(viam.isa().orElseThrow().pc()).registerResource();

    var ident = Identifier.noLocation("MiA");
    var bypass = bypass(ident);
    var predict = predict(ident);

    var fetch = fetch(ident.append("FETCH"));
    var decode = decode(ident.append("DECODE"), fetch.outputs().get(0), regFile, bypass);
    var execute = exec(ident.append("EXECUTE"), decode.outputs().get(0), pc, regFile, bypass);
    var memory = memory(ident.append("MEMORY"), execute.outputs().get(0), mem);
    var writeBack = writeBack(ident.append("WRITE_BACK"), memory.outputs().get(0), regFile);

    var mia = new MicroArchitecture(
        ident,
        mip,
        new ArrayList<>(List.of(fetch, decode, execute, memory, writeBack)),
        new ArrayList<>(List.of(bypass, predict))
    );

    viam.add(mia);

    viam.verify();

    return null;
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
   * stage FETCH -> ( fr : FetchResult ) =
   * {
   *   fr := fetchNext
   * }
   * </pre>.
   */
  private static Stage fetch(Identifier ident) {
    var fr = stageOutput(ident.append("fr"), MicroArchitectureType.fetchResult());
    return new Stage(ident, fetchBehavior(fr), List.of(fr));
  }

  private static Graph fetchBehavior(StageOutput fr) {
    var beh = new Graph("FETCH");
    var fn = new MiaBuiltInCall(BuiltInTable.FETCH_NEXT, new NodeList<>(),
        MicroArchitectureType.fetchResult());
    var wr = new WriteStageOutputNode(fr, fn);
    beh.addWithInputs(wr);
    return beh;
  }

  /**
   * <pre>
   * stage DECODE -> ( ir : Instruction ) =
   * {
   *   let instr = decode( FETCH.fr ) in
   *   {
   *     instr.address( @X )
   *     instr.readOrForward( @X, @bypass )
   *     ir := instr
   *   }
   * }
   * </pre>.
   */
  private static Stage decode(Identifier ident, StageOutput fetchIr, RegisterFile regFile,
                              Logic bypass) {
    var ir = stageOutput(ident.append("ir"), MicroArchitectureType.instruction());
    return new Stage(ident, decodeBehavior(fetchIr, ir, regFile, bypass), List.of(ir));
  }

  private static Graph decodeBehavior(StageOutput fetchIr, StageOutput ir, RegisterFile regFile,
                                      Logic bypass) {
    var rd = new ReadStageOutputNode(fetchIr);
    var i1 = new MiaBuiltInCall(BuiltInTable.DECODE, new NodeList<>(rd),
        MicroArchitectureType.instruction());
    var i2 = new MiaBuiltInCall(BuiltInTable.INSTRUCTION_ADDRESS, new NodeList<>(i1),
        MicroArchitectureType.instruction());
    i2.add(regFile);
    var i3 = new MiaBuiltInCall(BuiltInTable.INSTRUCTION_READ_OR_FORWARD, new NodeList<>(i2),
        MicroArchitectureType.instruction());
    i3.add(regFile);
    i3.add(bypass);
    var wr = new WriteStageOutputNode(ir, i3);
    var beh = new Graph("DECODE");
    beh.addWithInputs(wr);
    return beh;
  }

  /**
   * <pre>
   * stage EXECUTE -> ( ir : Instruction ) =
   * {
   *   let instr = DECODE.ir in
   *   {
   *     instr.read( @PC )
   *     instr.compute
   *     instr.verify
   *     instr.write( @PC )
   *     instr.results( @X, @bypass )
   *     ir := instr
   *   }
   * }
   * </pre>.
   */
  private static Stage exec(Identifier ident, StageOutput decodeIr, Resource pc,
                            RegisterFile regFile, Logic bypass) {
    var ir = stageOutput(ident.append("ir"), MicroArchitectureType.instruction());
    return new Stage(ident, executeBehavior(decodeIr, ir, pc, regFile, bypass), List.of(ir));
  }

  private static Graph executeBehavior(StageOutput decodeIr, StageOutput ir, Resource pc,
                                       RegisterFile regFile, Logic bypass) {
    var rd = new ReadStageOutputNode(decodeIr);
    var i1 = new MiaBuiltInCall(BuiltInTable.INSTRUCTION_READ, new NodeList<>(rd),
        MicroArchitectureType.instruction());
    i1.add(pc);
    var i2 = new MiaBuiltInCall(BuiltInTable.INSTRUCTION_COMPUTE, new NodeList<>(i1),
        MicroArchitectureType.instruction());
    var i3 = new MiaBuiltInCall(BuiltInTable.INSTRUCTION_VERIFY, new NodeList<>(i2),
        MicroArchitectureType.instruction());
    var i4 = new MiaBuiltInCall(BuiltInTable.INSTRUCTION_WRITE, new NodeList<>(i3),
        MicroArchitectureType.instruction());
    i4.add(pc);
    var i5 = new MiaBuiltInCall(BuiltInTable.INSTRUCTION_RESULTS, new NodeList<>(i4),
        MicroArchitectureType.instruction());
    i5.add(regFile);
    i5.add(bypass);
    var wr = new WriteStageOutputNode(ir, i5);
    var beh = new Graph("EXECUTE");
    beh.addWithInputs(wr);
    return beh;
  }

  /**
   * <pre>
   * stage MEMORY -> ( ir : Instruction ) =
   * {
   *   let instr = EXECUTE.ir in
   *   {
   *     instr.write( @MEM )
   *     instr.read( @MEM )
   *     ir := instr
   *   }
   * }
   * </pre>.
   */
  private static Stage memory(Identifier ident, StageOutput executeIr, Memory mem) {
    var ir = stageOutput(ident.append("ir"), MicroArchitectureType.instruction());
    return new Stage(ident, memoryBehavior(executeIr, ir, mem), List.of(ir));
  }

  private static Graph memoryBehavior(StageOutput executeIr, StageOutput ir, Memory mem) {
    var beh = new Graph("MEMORY");
    var rd = new ReadStageOutputNode(executeIr);
    var i1 = new MiaBuiltInCall(BuiltInTable.INSTRUCTION_WRITE, new NodeList<>(rd),
        MicroArchitectureType.instruction());
    i1.add(mem);
    var i2 = new MiaBuiltInCall(BuiltInTable.INSTRUCTION_READ, new NodeList<>(i1),
        MicroArchitectureType.instruction());
    i2.add(mem);
    var wr = new WriteStageOutputNode(ir, i2);
    beh.addWithInputs(wr);
    return beh;
  }

  /**
   * <pre>
   * stage WRITE_BACK =
   * {
   *   let instr = MEMORY.ir in
   *   {
   *     instr.write( @X )
   *   }
   * }
   * </pre>.
   */
  private static Stage writeBack(Identifier ident, StageOutput memoryIr, RegisterFile regFile) {
    return new Stage(ident, writeBackBehavior(memoryIr, regFile), Collections.emptyList());
  }

  private static Graph writeBackBehavior(StageOutput memoryIr, RegisterFile regFile) {
    var beh = new Graph("WRITE_BACK");
    var rd = new ReadStageOutputNode(memoryIr);
    var i1 = new MiaBuiltInCall(BuiltInTable.INSTRUCTION_WRITE, new NodeList<>(rd),
        MicroArchitectureType.instruction());
    i1.add(regFile);
    beh.addWithInputs(i1); // TODO ?
    return beh;
  }

}
