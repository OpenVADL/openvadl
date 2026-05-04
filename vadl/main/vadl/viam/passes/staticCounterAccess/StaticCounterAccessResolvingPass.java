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

import java.io.IOException;
import java.util.Set;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.ViamUtils;
import vadl.viam.Counter;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Specification;
import vadl.viam.graph.CanAccessCounter;
import vadl.viam.graph.Graph;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;

/**
 * The Static Counter Access Resolving Pass adds a {@link Counter} reference to some
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
 * Now giving a {@link WriteRegTensorNode} in a behavior, in general, we cannot determine
 * if the node writes the {@code PC} or not (e.g. if the index comes from a format field).
 * So most generators (such as the simulator) must add runtime checks to know if this node
 * actually writes to the PC.
 * However, in some cases we can statically know if the write-node writes the PC, because
 * of constant evaluation or because the user wrote {@code PC := ...}.
 * These two cases come down to the same, and this is exactly what this pass is doing.
 * It finds nodes that access the counter and adds a marker to such nodes.
 */
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

  private static void processRegisterNodes(Graph behavior, Counter regCounter) {
    behavior.getNodes(Set.of(ReadRegTensorNode.class, WriteRegTensorNode.class))
        .map(node -> (CanAccessCounter) node)
        .filter(node -> node.registerTensor().equals(regCounter.registerTensor()))
        .forEach(node -> node.setStaticCounterAccess(regCounter));
  }

  private static void processMultiDimRegisterNodes(Graph behavior,
                                                   Counter fileCounter) {
    behavior.getNodes(Set.of(ReadRegTensorNode.class, WriteRegTensorNode.class))
        .map(node -> (CanAccessCounter) node)
        .filter(node -> node.registerTensor().equals(fileCounter.registerTensor()))
        .filter(node -> {
          // check if the indices match
          int indexCount = node.indices().size();
          if (indexCount != fileCounter.indices().size()) {
            return false;
          }
          for (int i = 0; i < indexCount; i++) {
            if (node.indices().get(i) instanceof ConstantNode cn) {
              if (cn.constant().asVal().intValue() != fileCounter.indices().get(i).intValue()) {
                return false;
              }
            } else {
              // TODO: if any index cannot be statically evaluated, we must add an
              //       instr-translation-time check
              return false;
            }
          }
          return true;
        })
        .forEach(node -> node.setStaticCounterAccess(fileCounter));
  }

}
