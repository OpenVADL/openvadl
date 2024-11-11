package vadl.iss.passes;

import static java.util.Objects.requireNonNull;
import static vadl.utils.GraphUtils.binaryOp;
import static vadl.utils.GraphUtils.bitsNode;
import static vadl.utils.GraphUtils.getSingleNode;
import static vadl.utils.GraphUtils.readReg;
import static vadl.utils.GraphUtils.writeReg;

import java.io.IOException;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BuiltInTable;
import vadl.viam.Counter;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.graph.control.AbstractBeginNode;
import vadl.viam.graph.control.AbstractEndNode;
import vadl.viam.graph.control.BranchEndNode;
import vadl.viam.graph.control.ControlNode;
import vadl.viam.graph.control.ControlSplitNode;
import vadl.viam.graph.control.DirectionalNode;
import vadl.viam.graph.control.InstrEndNode;
import vadl.viam.graph.control.MergeNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.WriteRegNode;

/**
 * Inserts necessary PC (Program Counter) write operations into a control flow graph (CFG) when
 * generating code for QEMU.
 * <p>
 * <strong>Context:</strong><br>
 * In the QEMU Tiny Code Generator (TCG), when compiling guest instructions into host code,
 * it is crucial to ensure that all control flow paths correctly update the PC.
 * For conditional branches, if we do not emit a `goto_tb` for the default (no-branch) case,
 * QEMU cannot set `is_jmp` to `DISAS_NORETURN`, and it won't know the start of the
 * next Translation Block (TB).
 * By adding a PC write to the next instruction for branches that do not modify the PC,
 * we generate a `goto_tb` for that path, leading to correct behavior as QEMU now knows
 * it should branch to the next instruction (which may not yet be compiled).
 * </p>
 *
 * <p>
 * <strong>Algorithm Overview:</strong><br>
 * This class traverses the CFG and performs the following steps:
 * <ol>
 *   <li>Starts from an {@code AbstractBeginNode} and skips over any {@code DirectionalNode}s to
 *   focus on significant control nodes.</li>
 *   <li>Processes each {@code ControlSplitNode} (nodes where control flow diverges):
 *     <ul>
 *       <li>Recursively processes each branch stemming from the split.</li>
 *       <li>Determines if any branch writes to the PC.</li>
 *       <li>If any branch writes to the PC, inserts a PC write into branches that do not,
 *       ensuring all paths update the PC.</li>
 *     </ul>
 *   </li>
 *   <li>Ensures that all branches merge correctly at a {@code MergeNode}.</li>
 * </ol>
 * </p>
 *
 * <p>
 * <strong>Example Flow:</strong><br>
 * Consider the following control flow graph:
 * </p>
 *
 * <pre>
 *           +--------------------+
 *           |  AbstractBeginNode |
 *           +--------------------+
 *                     |
 *                     v
 *           +--------------------+
 *           |  ControlSplitNode  |
 *           +--------------------+
 *              /             \
 *             /               \
 *    +----------------+    +----------------+
 *    |   Branch A     |    |   Branch B     |
 *    | (PC is written)|    |(PC not written)|
 *    +----------------+    +----------------+
 *             \               /
 *              \             /
 *           +--------------------+
 *           |     MergeNode      |
 *           +--------------------+
 * </pre>
 *
 * <p>
 * In this graph, Branch A writes to the PC (e.g., a jump instruction), while Branch B does not.
 * This class will insert a PC write into Branch B to ensure that both branches update the PC.
 * This allows QEMU to generate appropriate `goto_tb` instructions for both branches.
 * </p>
 *
 * <p>
 * <strong>Note:</strong><br>
 * It is assumed that there cannot be two potential PC reads within the same sub-branch,
 * which should be guaranteed by prior passes in the compilation process.
 * </p>
 *
 * @deprecated Currently we use the DISAS_CHAIN strategy which will chain default branches
 *     so it isn't required anymore that each behavior branch has a PC write.
 */
// TODO: Consider removing as it seems to be not beneficial
@Deprecated
public class IssBranchPcWriteNormalizerPass extends Pass {

  public IssBranchPcWriteNormalizerPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("Iss Branch PC Write Normalizer Pass");
  }

  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam)
      throws IOException {
    var isa = viam.isa().get();
    var pc = (Counter.RegisterCounter) requireNonNull(isa.pc());

    isa.ownInstructions().forEach(i ->
        new IssBranchPcWriteNormalizer(i, pc).run()
    );
    return null;
  }

}

class IssBranchPcWriteNormalizer {

  StartNode instrStart;

  // for fast exit
  InstrEndNode instrEnd;

  Counter.RegisterCounter pc;
  Instruction instruction;

  public IssBranchPcWriteNormalizer(Instruction instruction, Counter.RegisterCounter pc) {
    this.instrStart = getSingleNode(instruction.behavior(), StartNode.class);
    this.instrEnd = getSingleNode(instruction.behavior(), InstrEndNode.class);
    ;
    this.instruction = instruction;
    this.pc = pc;
  }

  void run() {
    var anyPcWrite = instruction.behavior()
        .getNodes(WriteRegNode.class)
        .anyMatch(WriteRegNode::isPcAccess);
    if (!anyPcWrite) {
      // the graph has no PC write, we can skip it entirely
      return;
    }

    if (hasPcWrite(instrEnd)) {
      // there is a PC write at the end in all branches
      // this means that there is non in any oder branch
      return;
    }

    // run for main branch
    runForBranch(instrStart);
  }

  /**
   * Processes a branch starting from the given {@code AbstractBeginNode},
   * ensuring that the PC is correctly updated.
   * <p>
   * This method traverses the control flow of a branch, handling any control splits and merges.
   * It recursively processes nested branches, determines if any of them write to the PC,
   * and inserts PC write operations
   * into branches that do not, to ensure consistent behavior during code generation for QEMU.
   * </p>
   *
   * @param begin the starting node of the branch to process
   * @return a {@code BranchResult} containing whether the branch writes to the PC
   *     and the end node of the branch
   */
  private BranchResult runForBranch(AbstractBeginNode begin) {
    // continue and skip all directionals
    var currNode = skipAllDirectionals(begin);

    var someWriteOnBranch = false;

    while (currNode instanceof ControlSplitNode ctrSplit) {
      // process all control split nodes.
      // note that this shouldn't be necessary as there cannot be two potential
      // reg reads within the same sub branch (like two if statements with one PC write each).
      // this should be guaranteed by some pass before.

      var pcWritesPerBranch = ctrSplit.branches().stream()
          .map(this::runForBranch)
          .toList();

      var anyPcWriteInCtrSplit = pcWritesPerBranch.stream().anyMatch(BranchResult::hasPcWrite);

      if (anyPcWriteInCtrSplit) {
        someWriteOnBranch = true;
        // we have to add a pc write to every branch that does not have a PC write
        pcWritesPerBranch.forEach(r -> {
          if (!r.hasPcWrite) {
            // branch has no pc write -> add it
            // the PC will point to the next instruction
            addPcWrite(r.endNode);
          }
        });
      }

      var mergeNode =
          getMergeNodeOf(ctrSplit, pcWritesPerBranch.stream().map(BranchResult::endNode));

      // get to next non directional
      currNode = skipAllDirectionals(mergeNode);
    }

    // now we should have a control split node in place
    currNode.ensure(currNode instanceof AbstractEndNode,
        "Unexpected node. Expected AbstractEndNode");

    var branchEndNode = (AbstractEndNode) currNode;

    if (!someWriteOnBranch) {
      someWriteOnBranch = hasPcWrite(branchEndNode);
    }

    return new BranchResult(someWriteOnBranch, branchEndNode);
  }


  /**
   * Skips over any {@code DirectionalNode}s starting from the given node,
   * moving to the next significant control node.
   *
   * @param node the starting control node
   * @return the next non-directional control node
   */
  private ControlNode skipAllDirectionals(ControlNode node) {
    while (node instanceof DirectionalNode dirNode) {
      node = dirNode.next();
    }
    return node;
  }


  /**
   * Holds the result of processing a branch, including whether it writes to
   * the PC and its end node.
   *
   * @param hasPcWrite indicates if the branch writes to the PC
   * @param endNode    the end node of the branch
   */
  private record BranchResult(
      boolean hasPcWrite,
      AbstractEndNode endNode
  ) {
  }

  /**
   * Adds a PC write operation to the given end node.
   * <p>
   * The new PC write will point the PC to the next instruction,
   * ensuring that QEMU can generate a {@code goto_tb}
   * for this branch, which is essential for correct execution when
   * the next translation block is not yet compiled.
   * </p>
   *
   * @param endNode the end node to which the PC write will be added
   */
  private void addPcWrite(AbstractEndNode endNode) {
    var pcReg = pc.registerRef();
    // Generate PC := PC + <instr_width>
    var pcWrite = instruction.behavior().addWithInputs(
        writeReg(
            pcReg,
            binaryOp(BuiltInTable.ADD, pcReg.type(),
                readReg(pcReg, pc),
                bitsNode(instruction.format().type().bitWidth() / 8,
                    pcReg.resultType().bitWidth())
            ),
            pc
        ));

    endNode.addSideEffect(pcWrite);
  }

  /**
   * Checks if the given end node contains a PC write operation.
   *
   * @param endNode the end node to check
   * @return {@code true} if the end node has a PC write, {@code false} otherwise
   */
  private boolean hasPcWrite(AbstractEndNode endNode) {
    return endNode.sideEffects().stream()
        .filter(s -> s instanceof WriteRegNode)
        .anyMatch(s -> ((WriteRegNode) s).isPcAccess());
  }

  /**
   * Retrieves the {@code MergeNode} corresponding to the provided control split node.
   * <p>
   * Ensures that all branches from the control split converge at the same merge node,
   * which is necessary for consistent control flow and correct code generation.
   * </p>
   *
   * @param ctrSplit the control split node
   * @param withEnds a stream of end nodes from each branch
   * @return the common {@code MergeNode} where the branches converge
   * @throws IllegalStateException if branches are merged with different merge nodes,
   *                               or if end nodes are not used by a merge node
   */
  private MergeNode getMergeNodeOf(ControlSplitNode ctrSplit, Stream<AbstractEndNode> withEnds) {
    var mergeNodes = withEnds.map(e -> {
      var usages = e.usages().toList();
      e.ensure(usages.size() == 1,
          "Unexpected usage, expected to have exactly one usage: MergeNode. But was %s",
          usages);
      return usages.get(0);
    }).distinct().toList();

    ctrSplit.ensure(mergeNodes.size() == 1,
        "Branches are merged with different merge nodes. Got: %s", mergeNodes);
    var mergeNode = mergeNodes.get(0);
    ctrSplit.ensure(mergeNode instanceof MergeNode, "Branch ends not used by merge node but %s",
        mergeNode);
    return (MergeNode) mergeNode;
  }

}
