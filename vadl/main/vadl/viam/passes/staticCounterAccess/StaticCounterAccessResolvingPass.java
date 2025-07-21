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
import vadl.viam.DefProp;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Specification;
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

    ViamUtils.findDefinitionsByFilter(viam, d -> d instanceof DefProp.WithBehavior)
        .stream()
        .map(DefProp.WithBehavior.class::cast)
        .flatMap(d -> d.behaviors().stream())
        .forEach(behavior -> resolveInBehavior(behavior, pc));
    return null;
  }

  private static void resolveInBehavior(Graph behavior, Counter counter) {
    if (counter.registerTensor().isSingleRegister()) {
      // if the counter is a register counter we only look at the register read/write nodes
      processRegisterNodes(behavior, counter);
    } else {
      // if the counter is a register file counter we only look at the register file read/write
      // nodes
      processRegisterFileNodes(behavior, counter);
    }
  }

  private static void processRegisterNodes(Graph behavior, Counter regCounter) {
    behavior.getNodes(Set.of(ReadRegTensorNode.class, WriteRegTensorNode.class))
        .forEach(node -> {
          if (node instanceof ReadRegTensorNode read
              && read.regTensor() == regCounter.registerTensor()) {
            // if the node is a read and
            // the register file matches the register file of the counter
            // we set the static counter access field of the read node
            read.setStaticCounterAccess(regCounter);

          } else if (node instanceof WriteRegTensorNode write
              && write.regTensor() == regCounter.registerTensor()) {
            // if the node is a write and
            // the register file matches the register file of the counter
            // we set the static counter access field of the write node
            write.setStaticCounterAccess(regCounter);
          }
        });
  }

  private static void processRegisterFileNodes(Graph behavior,
                                               Counter fileCounter) {
    // TODO: Generalize this
    if (fileCounter.indices().size() != 1) {
      return;
    }

    // get all register file read and write nodes
    behavior.getNodes(Set.of(ReadRegTensorNode.class, WriteRegTensorNode.class))
        .forEach(node -> {

          if (node instanceof ReadRegTensorNode read
              && read.regTensor() == fileCounter.registerTensor()
              && read.indices().getFirst() instanceof ConstantNode constIndex
              && constIndex.constant().asVal().intValue()
              == fileCounter.indices().getFirst().intValue()) {
            // if the node is a read and
            // the register file matches the register file of the counter and
            // the address(index) of the read is constant and
            // and the value of the address is the same as the one of the counter's index
            // we set the static counter access field of the read node

            read.setStaticCounterAccess(fileCounter);

          } else if (node instanceof WriteRegTensorNode write
              && write.regTensor() == fileCounter.registerTensor()
              && write.indices().getFirst() instanceof ConstantNode constIndex
              && constIndex.constant().asVal().intValue()
              == fileCounter.indices().getFirst().intValue()) {

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
