package vadl.viam.passes.dummyPasses;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BuiltInTable;
import vadl.types.DataType;
import vadl.types.MicroArchitectureType;
import vadl.viam.*;
import vadl.viam.graph.Graph;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.MiaBuiltInCall;
import vadl.viam.graph.dependency.ReadRegNode;
import vadl.viam.graph.dependency.WriteRegNode;

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
    var decode = decode(ident.append("DECODE"), (Register) fetch.outputs().get(0), regFile, bypass);
    var execute = execute(ident.append("EXECUTE"), (Register) decode.outputs().get(0), pc, regFile, bypass);
    var memory = memory(ident.append("MEMORY"), (Register) execute.outputs().get(0), mem);
    var writeBack = writeBack(ident.append("WRITE_BACK"), (Register) memory.outputs().get(0), regFile);

    var mia = new MicroArchitecture(
        ident,
        mip,
        List.of(fetch, decode, execute, memory, writeBack),
        List.of(bypass, predict)
    );

    viam.add(mia);

    viam.verify();

    return null;
  }

  private static Register pipelineReg(Identifier ident, DataType type) {
    return new Register(ident, type, Register.AccessKind.FULL, Register.AccessKind.FULL,
            null, new Register[]{});
  }

  /**
   * [forwarding]
   * logic bypass
   */
  private static Logic.Forwarding bypass(Identifier parent) {
    var id = parent.append("bypass");
    return new Logic.Forwarding(id);
  }

  /**
   * [branch predictor]
   * logic predict
   */
  private static Logic.BranchPrediction predict(Identifier parent) {
    var id = parent.append("predict");
    return new Logic.BranchPrediction(id);
  }

  /**
   * stage FETCH -> ( fr : FetchResult ) =
   * {
   *     fr := fetchNext
   * }
   */
  private static Stage fetch(Identifier ident) {
    var fr = pipelineReg(ident.append("fr"), MicroArchitectureType.fetchResult());
    return new Stage(ident, fetchBehavior(fr), Collections.emptyList(), List.of(fr));
  }

  private static Graph fetchBehavior(Register fr) {
    var beh = new Graph("FETCH");
    var fn = new MiaBuiltInCall(BuiltInTable.FETCH_NEXT, new NodeList<>(), MicroArchitectureType.fetchResult());
    var wr = new WriteRegNode(fr, fn, null);
    beh.addWithInputs(wr);
    return beh;
  }

  /**
   * stage DECODE -> ( ir : Instruction ) =
   * {
   *     let instr = decode( FETCH.fr ) in
   *     {
   *         instr.address( @X )
   *         instr.readOrForward( @X, @bypass )
   *         ir := instr
   *     }
   * }
   */
  private static Stage decode(Identifier ident, Register fetchIr, RegisterFile regFile, Logic bypass) {
    var ir = pipelineReg(ident.append("ir"), MicroArchitectureType.instruction());
    return new Stage(ident, decodeBehavior(fetchIr, ir, regFile, bypass), Collections.emptyList(), List.of(ir));
  }

  private static Graph decodeBehavior(Register fetchIr, Register ir, RegisterFile regFile, Logic bypass) {
    var beh = new Graph("DECODE");
    var rd = new ReadRegNode(fetchIr, MicroArchitectureType.fetchResult(), null);
    var i1 = new MiaBuiltInCall(BuiltInTable.DECODE, new NodeList<>(rd), MicroArchitectureType.instruction());
    var i2 = new MiaBuiltInCall(BuiltInTable.INSTRUCTION_ADDRESS, new NodeList<>(i1), MicroArchitectureType.instruction());
    i2.add(regFile);
    var i3 = new MiaBuiltInCall(BuiltInTable.INSTRUCTION_READ_OR_FORWARD, new NodeList<>(i2), MicroArchitectureType.instruction());
    i3.add(regFile);
    i3.add(bypass);
    var wr = new WriteRegNode(ir, i3, null);
    beh.addWithInputs(wr);
    return beh;
  }

  /**
   * stage EXECUTE -> ( ir : Instruction ) =
   * {
   *     let instr = DECODE.ir in
   *     {
   *         instr.read( @PC )
   *         instr.compute
   *         instr.verify
   *         instr.write( @PC )
   *         instr.results( @X, @bypass )
   *         ir := instr
   *     }
   * }
   */
  private static Stage execute(Identifier ident, Register decodeIr, Resource pc, RegisterFile regFile, Logic bypass) {
    var ir = pipelineReg(ident.append("ir"), MicroArchitectureType.instruction());
    return new Stage(ident, executeBehavior(decodeIr, ir, pc, regFile, bypass), Collections.emptyList(), List.of(ir));
  }

  private static Graph executeBehavior(Register decodeIr, Register ir, Resource pc, RegisterFile regFile, Logic bypass) {
    var beh = new Graph("EXECUTE");
    var rd = new ReadRegNode(decodeIr, MicroArchitectureType.instruction(), null);
    var i1 = new MiaBuiltInCall(BuiltInTable.INSTRUCTION_READ, new NodeList<>(rd), MicroArchitectureType.instruction());
    i1.add(pc);
    var i2 = new MiaBuiltInCall(BuiltInTable.INSTRUCTION_COMPUTE, new NodeList<>(i1), MicroArchitectureType.instruction());
    var i3 = new MiaBuiltInCall(BuiltInTable.INSTRUCTION_VERIFY, new NodeList<>(i2), MicroArchitectureType.instruction());
    var i4 = new MiaBuiltInCall(BuiltInTable.INSTRUCTION_WRITE, new NodeList<>(i3), MicroArchitectureType.instruction());
    i4.add(pc);
    var i5 = new MiaBuiltInCall(BuiltInTable.INSTRUCTION_RESULTS, new NodeList<>(i4), MicroArchitectureType.instruction());
    i5.add(regFile);
    i5.add(bypass);
    var wr = new WriteRegNode(ir, i5, null);
    beh.addWithInputs(wr);
    return beh;
  }

  /**
   * stage MEMORY -> ( ir : Instruction ) =
   * {
   *     let instr = EXECUTE.ir in
   *     {
   *         instr.write( @MEM )
   *         instr.read( @MEM )
   *         ir := instr
   *     }
   * }
   */
  private static Stage memory(Identifier ident, Register executeIr, Memory mem) {
    var ir = pipelineReg(ident.append("ir"), MicroArchitectureType.instruction());
    return new Stage(ident, memoryBehavior(executeIr, ir, mem), Collections.emptyList(), List.of(ir));
  }

  private static Graph memoryBehavior(Register executeIr, Register ir, Memory mem) {
    var beh = new Graph("MEMORY");
    var rd = new ReadRegNode(executeIr, MicroArchitectureType.instruction(), null);
    var i1 = new MiaBuiltInCall(BuiltInTable.INSTRUCTION_WRITE, new NodeList<>(rd), MicroArchitectureType.instruction());
    i1.add(mem);
    var i2 = new MiaBuiltInCall(BuiltInTable.INSTRUCTION_READ, new NodeList<>(i1), MicroArchitectureType.instruction());
    i2.add(mem);
    var wr = new WriteRegNode(ir, i2, null);
    beh.addWithInputs(wr);
    return beh;
  }

  /**
   * stage WRITE_BACK =
   * {
   *     let instr = MEMORY.ir in
   *     {
   *         instr.write( @X )
   *     }
   * }
   */
  private static Stage writeBack(Identifier ident, Register memoryIr, RegisterFile regFile) {
    return new Stage(ident, writeBackBehavior(memoryIr, regFile), Collections.emptyList(), Collections.emptyList());
  }

  private static Graph writeBackBehavior(Register memoryIr, RegisterFile regFile) {
    var beh = new Graph("WRITE_BACK");
    var rd = new ReadRegNode(memoryIr, MicroArchitectureType.instruction(), null);
    var i1 = new MiaBuiltInCall(BuiltInTable.INSTRUCTION_WRITE, new NodeList<>(rd), MicroArchitectureType.instruction());
    i1.add(regFile);
    beh.addWithInputs(i1); // TODO ?
    return beh;
  }

}
