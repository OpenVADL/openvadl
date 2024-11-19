package vadl.lcb.passes.llvmLowering.tablegen.lowering;

import static vadl.viam.ViamError.ensure;

import java.util.BitSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionNode;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbPseudoInstructionNode;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenMachineInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPseudoInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenSelectionPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenSelectionWithOutputPattern;
import vadl.viam.Instruction;
import vadl.viam.PseudoInstruction;

/**
 * Utility class for mapping into TableGen. But it only prints the anonymous patterns.
 * The split between {@link TableGenInstructionPatternRenderer} and
 * {@link TableGenInstructionRenderer} is required because TableGen does not allow forward
 * declarations. Therefore, all instructions must be defined before they can be used in patterns.
 * This might be problem for some patterns.
 */
public final class TableGenInstructionPatternRenderer {
  private static final Logger logger = LoggerFactory.getLogger(
      TableGenInstructionPatternRenderer.class);

  /**
   * Transforms the given {@link Instruction} into a string which can be used by LLVM's TableGen.
   * It will *ONLY* print the anonymous pattern if the pattern is actually lowerable.
   */
  public static String lower(TableGenMachineInstruction instruction) {
    var anonymousPatterns = instruction.getAnonymousPatterns().stream()
        .filter(TableGenPattern::isPatternLowerable)
        .filter(x -> x instanceof TableGenSelectionWithOutputPattern)
        .map(x -> (TableGenSelectionWithOutputPattern) x)
        .toList();
    return String.format("""
            %s
            """,
        anonymousPatterns
            .stream()
            .map(x -> lower(instruction, x))
            .collect(Collectors.joining("\n"))
    );
  }

  /**
   * Transforms the given {@link PseudoInstruction} into a string which can be used by LLVM's
   * TableGen.
   */
  public static String lower(TableGenPseudoInstruction instruction) {
    var anonymousPatterns = instruction.getAnonymousPatterns().stream()
        .filter(TableGenPattern::isPatternLowerable)
        .filter(x -> x instanceof TableGenSelectionWithOutputPattern)
        .map(x -> (TableGenSelectionWithOutputPattern) x)
        .toList();
    var y = String.format("""
            %s
            """,
        anonymousPatterns.stream()
            .map(x -> lower(instruction, x))
            .collect(Collectors.joining("\n"))
    );

    return y;
  }

  private static String lower(TableGenSelectionPattern tableGenPattern) {
    ensure(tableGenPattern.isPatternLowerable(), "TableGen pattern must be lowerable");
    var visitor = new TableGenPatternPrinterVisitor();

    for (var root : tableGenPattern.selector().getDataflowRoots()) {
      visitor.visit(root);
    }

    return "(" + visitor.getResult() + ")";
  }

  private static String lower(TableGenInstruction instruction,
                              TableGenSelectionWithOutputPattern tableGenPattern) {
    logger.atTrace().log("Lowering pattern for " + instruction.getName());
    ensure(tableGenPattern.isPatternLowerable(), "TableGen pattern must be lowerable");
    var visitor = new TableGenPatternPrinterVisitor();
    var machineVisitor = new TableGenMachineInstructionPrinterVisitor();

    for (var root : tableGenPattern.selector().getDataflowRoots()) {
      visitor.visit(root);
    }

    for (var root : tableGenPattern.machine().getDataflowRoots()) {
      ensure(root instanceof LcbPseudoInstructionNode
              || root instanceof LcbMachineInstructionNode,
          "root node must be pseudo or machine node");
      if (root instanceof LcbMachineInstructionNode machineInstructionNode) {
        machineVisitor.visit(machineInstructionNode);
      } else {
        LcbPseudoInstructionNode pseudoInstructionNode = (LcbPseudoInstructionNode) root;
        machineVisitor.visit(pseudoInstructionNode);
      }
    }

    return String.format("""
        def : Pat<%s,
                %s>;
        """, visitor.getResult(), machineVisitor.getResult());

  }

  /**
   * Converts a bitset into string representation.
   *
   * @param bitSet bitset
   * @param size   the real size of the {@code bitSet}. {@code bitSet} returns only
   *               the highest bit + 1.
   * @return "01010000" binary string
   */
  @Nullable
  private static String toBinaryString(BitSet bitSet, int size) {
    if (bitSet == null) {
      return null;
    }
    return IntStream.range(0, size)
        .mapToObj(b -> String.valueOf(bitSet.get(b) ? 1 : 0))
        .collect(Collectors.joining());
  }
}
