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

package vadl.viam.passes.staticCounterAccess;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.error.Diagnostic;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BuiltInTable;
import vadl.utils.GraphUtils;
import vadl.utils.Pair;
import vadl.utils.ViamUtils;
import vadl.utils.WithLocation;
import vadl.viam.Constant;
import vadl.viam.Counter;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Specification;
import vadl.viam.ViamError;
import vadl.viam.graph.CanAccessCounter;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.AbstractEndNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.SelectNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;

/**
 * The Counter Access Resolving Pass adds a {@link Counter} reference to some
 * register(-file) read/write nodes.
 * Essentially it tries to mark nodes that access a {@link Counter} (mostly the program counter),
 * so generators like the LCB can determine if the user intended a PC read/write or not
 * (e.g. {@link WriteRegTensorNode#staticCounterAccess()} returns the (nullable) counter that
 * it writes to).
 *
 * <p>You might ask why we need this, as the user can use the {@code program counter} definition
 * so it should always be clear if a node accesses the PC.
 * However, users can also use alias definitions like
 * {@code alias program counter PC: Regs = X(31)}, which
 * means that the program counter is one register in the register file {@code X}.
 * Now given a {@link WriteRegTensorNode} in a behavior, we can statically determine if it
 * writes the {@code PC}, if all the indices used are constant (or when the user wrote
 * {@code PC := ...}). If they are not (e.g. if
 * they come from a format field), we insert a runtime check: We duplicate the node, mark the
 * duplicate as a {@code PC} access and insert a runtime condition that decides which node
 * is used. The runtime condition compares the indices of the node to the indices of the
 * program counter. Now we can statically determine for each of the nodes, if they are
 * a {@code PC} access.
 */
public class CounterAccessResolvingPass extends Pass {

  public CounterAccessResolvingPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("Resolve Counter Accesses");
  }

  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam)
      throws IOException {

    var pc = viam.isa()
        .map(InstructionSetArchitecture::pc)
        .orElse(null);

    if (pc == null) {
      // if we got no PC we have nothing to resolve!
      return null;
    }

    ViamUtils.findAllBehaviors(viam).forEach(behavior -> resolveInBehavior(behavior, pc));
    return null;
  }

  private static void resolveInBehavior(Graph behavior, Counter counter) {
    if (counter.registerTensor().isSingleRegister()) {
      // if the counter is a register counter we only look at the register read/write nodes
      processRegisterNodes(behavior, counter);
    } else {
      // the counter is part of a register file or tensor
      processMultiDimRegisterNodes(behavior, counter);
    }
  }

  private static void processRegisterNodes(Graph behavior, Counter counter) {
    behavior.getNodes(Set.of(ReadRegTensorNode.class, WriteRegTensorNode.class))
        .map(node -> (CanAccessCounter) node)
        .filter(node -> node.registerTensor().equals(counter.registerTensor()))
        .forEach(node -> node.setStaticCounterAccess(counter));
  }

  private static void processMultiDimRegisterNodes(Graph behavior,
                                                   Counter counter) {
    // save nodes, because we might add some later
    var accesses = behavior
        .getNodes(Set.of(ReadRegTensorNode.class, WriteRegTensorNode.class))
        .toList();
    accesses.stream()
        .map(node -> (CanAccessCounter) node)
        .filter(node -> node.registerTensor().equals(counter.registerTensor()))
        .forEach(node -> handleIndexedAccess(node, counter));
  }

  private static void handleIndexedAccess(CanAccessCounter node, Counter counter) {
    var indexCount = node.indices().size();

    // check if the indices match
    if (indexCount != counter.indices().size()) {
      // FIXME: what if we write to multiple registers (eg by leaving out the last index)
      //        is that possible? If yes, then this check fails to notice a PC change!
      throw Diagnostic.error("Program counter access must be single register",
          (WithLocation) node)
          .description("This register access involves multiple registers. One or multiple "
              + "of them could be program counter accesses. However we currently do not "
              + "support such accesses.")
          .help("Split the register file/tensor read/write into individual operations per "
              + "register.")
          .note("We have to implement this!").build();
    }

    var nonConstPairs = new ArrayList<Pair<ExpressionNode, Constant.Value>>();
    for (int i = 0; i < indexCount; i++) {
      var accessIdx = node.indices().get(i);
      var counterIdx = counter.indices().get(i);

      if (accessIdx instanceof ConstantNode cn) {
        if (cn.constant().asVal().intValue() != counterIdx.intValue()) {
          // different index
          return;
        }
      } else {
        nonConstPairs.add(Pair.of(accessIdx, counterIdx));
      }
    }

    if (nonConstPairs.isEmpty()) {
      // indices are constant and match
      node.setStaticCounterAccess(counter);
    } else {
      // some (or all) indices are not constant -> insert runtime check
      // if this condition is true, then all indices match and it is a counter access
      var condition = counterAccessCondition(nonConstPairs);
      switch (node) {
        case ReadRegTensorNode read -> handleRead(read, counter, condition);
        case WriteRegTensorNode write -> handleWrite(write, counter, condition);
        default -> throw new ViamError(
            "Counter access resolving not implemented for: " + node.getClass().getSimpleName()
        );
      }
    }
  }

  private static void handleRead(ReadRegTensorNode read, Counter counter, ExpressionNode cond) {
    var counterRead = read.shallowCopy();
    counterRead.setStaticCounterAccess(counter);
    read.replace(new SelectNode(cond, counterRead, read)).setSourceLocation(read.location());
  }

  private static void handleWrite(WriteRegTensorNode write, Counter counter, ExpressionNode cond) {
    var counterWrite = write.shallowCopy();
    counterWrite.setStaticCounterAccess(counter);
    var next = GraphUtils.getSingleUsage(write, AbstractEndNode.class);
    next.removeSideEffect(write);
    var prev = requireNonNull(next.predecessor());
    prev.unlinkNext();
    prev.setNext(GraphUtils.ifElseSideEffect(
        requireNonNull(write.graph()),
        cond,
        List.of(counterWrite),
        List.of(write),
        next,
        write.location()
    ));
  }

  private static ExpressionNode counterAccessCondition(
      List<Pair<ExpressionNode, Constant.Value>> nonConstIndices) {
    return nonConstIndices.stream()
        .map(pair -> BuiltInCall.of(BuiltInTable.EQU, pair.left(), pair.right().toNode()))
        .reduce((first, second) -> BuiltInCall.of(BuiltInTable.AND, first, second)).get();
  }

}
