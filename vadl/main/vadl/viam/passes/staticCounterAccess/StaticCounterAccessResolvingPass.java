package vadl.viam.passes.staticCounterAccess;

import java.io.IOException;
import java.util.Set;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.ViamUtils;
import vadl.viam.Counter;
import vadl.viam.DefProp;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.ReadRegNode;
import vadl.viam.graph.dependency.WriteRegFileNode;
import vadl.viam.graph.dependency.WriteRegNode;

public class StaticCounterAccessResolvingPass extends Pass {

  public StaticCounterAccessResolvingPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("Resolve Counter Accesses");
  }

  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam)
      throws IOException {

    var pcs = viam.isas()
        .map(InstructionSetArchitecture::pc)
        .distinct()
        .toList();

    // TODO: Refactor this as soon as we only have a single ISA per specification
    viam.ensure(pcs.size() <= 1,
        "Only a single PC must be used per specification. Couldn't derive which one to use from %s",
        pcs);

    if (pcs.isEmpty()) {
      // if we got no PC we have nothing to resolve!
      return null;
    }

    var pc = pcs.get(0);

    ViamUtils.findDefinitionByFilter(viam, d -> d instanceof DefProp.WithBehavior)
        .stream()
        .map(DefProp.WithBehavior.class::cast)
        .flatMap(d -> d.behaviors().stream())
        .forEach(behavior -> resolveInBehavior(behavior, pc));
    return null;
  }

  private static void resolveInBehavior(Graph behavior, Counter counter) {
    if (counter instanceof Counter.RegisterCounter regCounter) {
      // if the counter is a register counter we only look at the register read/write nodes
      processRegisterNodes(behavior, regCounter);
    } else if (counter instanceof Counter.RegisterFileCounter fileCounter) {
      // if the counter is a register file counter we only look at the register file read/write
      // nodes
      processRegisterFileNodes(behavior, fileCounter);
    }
  }

  private static void processRegisterNodes(Graph behavior, Counter.RegisterCounter regCounter) {
    behavior.getNodes(Set.of(ReadRegNode.class, WriteRegNode.class))
        .forEach(node -> {
          if (node instanceof ReadRegNode read && read.register() == regCounter.registerRef()) {
            // if the node is a read and
            // the register file matches the register file of the counter
            // we set the static counter access field of the read node
            read.setStaticCounterAccess(regCounter);

          } else if (node instanceof WriteRegNode write &&
              write.register() == regCounter.registerRef()) {
            // if the node is a write and
            // the register file matches the register file of the counter
            // we set the static counter access field of the write node
            write.setStaticCounterAccess(regCounter);
          }
        });
  }

  private static void processRegisterFileNodes(Graph behavior,
                                               Counter.RegisterFileCounter fileCounter) {
    // get all register file read and write nodes
    behavior.getNodes(Set.of(ReadRegFileNode.class, WriteRegFileNode.class))
        .forEach(node -> {

          if (node instanceof ReadRegFileNode read
              && read.registerFile() == fileCounter.registerFileRef()
              && read.address() instanceof ConstantNode constIndex
              && constIndex.constant().asVal().intValue() == fileCounter.index().intValue()) {
            // if the node is a read and
            // the register file matches the register file of the counter and
            // the address(index) of the read is constant and
            // and the value of the address is the same as the one of the counter's index
            // we set the static counter access field of the read node

            read.setStaticCounterAccess(fileCounter);

          } else if (node instanceof WriteRegFileNode write
              && write.registerFile() == fileCounter.registerFileRef()
              && write.address() instanceof ConstantNode constIndex
              && constIndex.constant().asVal().intValue() == fileCounter.index().intValue()) {

            // if the node is a write and
            // the register file matches the register file of the counter and
            // the address(index) of the write is constant and
            // and the value of the address is the same as the one of the counter's index
            // we set the static counter access field of the write node
            write.setStaticCounterAccess(fileCounter);

          }
        });
  }

}
