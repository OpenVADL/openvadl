package vadl.iss.passes;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Instruction;
import vadl.viam.Memory;
import vadl.viam.Register;
import vadl.viam.RegisterFile;
import vadl.viam.Resource;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.ReadResourceNode;

public class IssReadVarAssignPass extends Pass {


  public record Result(
      Map<ReadResourceNode, TcgV> assignments
  ) {
  }

  public IssReadVarAssignPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  public IssConfiguration configuration() {
    return (IssConfiguration) super.configuration();
  }

  @Override
  public PassName getName() {
    return PassName.of("ISS Read Variable Assignment Pass");
  }

  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam)
      throws IOException {
    var result = new Result(new HashMap<>());
    viam.isa().ifPresent(
        isa -> isa.ownInstructions().forEach(
            instr -> IssReadVarAssigner.runFor(instr, result, configuration())));
    return result;
  }

}


class IssReadVarAssigner {

  Map<ReadResourceNode, TcgV> assignments;
  Map<Resource, Integer> resourceCounter;
  IssConfiguration configuration;

  IssReadVarAssigner(
      Map<ReadResourceNode, TcgV> assignments,
      IssConfiguration configuration
  ) {
    this.assignments = assignments;
    this.resourceCounter = new HashMap<>();
    this.configuration = configuration;
  }

  static void runFor(Instruction instruction, IssReadVarAssignPass.Result result,
                     IssConfiguration configuration) {
    var assigner = new IssReadVarAssigner(result.assignments(), configuration);
    instruction.behavior().getNodes(ReadResourceNode.class).forEach(assigner::assign);
  }

  private void assign(ReadResourceNode resRead) {
    var res = resRead.resourceDefinition();
    var name = getUniqueName(res);

    var var = TcgV.of(name, configuration.targetSize());

    assignments.put(resRead, var);
  }

  private String getUniqueName(Resource res) {
    if (res instanceof Register reg) {
      // no counter as all register reads are unique nodes (only one node for the same reg)
      return "reg_" + reg.simpleName();
    } else if (res instanceof RegisterFile reg) {
      return "regfile_" + reg.simpleName() + "_" + getAndIncCnt(reg);
    } else if (res instanceof Memory mem) {
      return "mem_" + mem.simpleName() + "_" + getAndIncCnt(mem);
    } else {
      throw new IllegalArgumentException("unknown resource type: " + res);
    }
  }

  private int getAndIncCnt(Resource res) {
    return resourceCounter.compute(res, (k, v) -> (v == null) ? 1 : v + 1);
  }

}


