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

package vadl.gcb.passes;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.CompilerInstruction;
import vadl.viam.Instruction;
import vadl.viam.PseudoInstruction;
import vadl.viam.RegisterTensor;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.WriteArtificialResNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;
import vadl.viam.graph.dependency.WriteResourceNode;

/**
 * Determine the register uses and defs of an {@link Instruction} and {@link PseudoInstruction}.
 */
public class DetermineRegisterUsesAndDefsPass extends Pass {
  public DetermineRegisterUsesAndDefsPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("DetermineRegisterUsesAndDefsPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var abi = viam.abi().orElseThrow();

    var machine = new IdentityHashMap<Instruction, Info>();
    var pseudo = new IdentityHashMap<PseudoInstruction, Info>();
    var compiler = new IdentityHashMap<CompilerInstruction, Info>();

    // Machine Instructions
    for (var instruction : viam.isa().orElseThrow().ownInstructions()) {
      var uses = getRegisterUses(instruction.behavior());
      var defs = getRegisterDefs(instruction.behavior());

      var info = new Info(uses, defs);
      machine.put(instruction, info);
    }

    // Pseudo Instructions
    for (var pseudoInstruction : viam.isa().orElseThrow().ownPseudoInstructions()) {
      var info = applyArgumentsAndCreateInfo(pseudoInstruction);
      pseudo.put(pseudoInstruction, info);
    }

    // Compiler Instructions
    var compilerInstructions = Stream.concat(abi.constantSequences().stream(),
        abi.registerAdjustmentSequences().stream()).toList();
    for (var compilerInstruction : compilerInstructions) {
      var info = applyArgumentsAndCreateInfo(compilerInstruction);
      compiler.put(compilerInstruction, info);
    }

    return new Output(machine, compiler, pseudo);
  }

  /**
   * Imagine that you have a pseudo instruction which hardcodes the register. We need to replace
   * the fields in the instruction and compute the defs and uses again. We must not reuse the result
   * from the machine instructions.
   * <pre>
   * pseudo instruction CALL( symbol : Bits<32> ) =
   * {
   *  LUI{ rd = 1 as Bits5, imm = hi( symbol ) }
   *  JALR{ rd = 1 as Bits5, rs1 = 1 as Bits5, imm = lo( symbol ) }
   * }
   * </pre>
   */
  private Info applyArgumentsAndCreateInfo(CompilerInstruction instruction) {
    var uses = new HashSet<RegisterRef>();
    var defs = new HashSet<RegisterRef>();
    for (var instrCallNode : instruction.behavior().getNodes(InstrCallNode.class).map(
        x -> (InstrCallNode) x.copy()).toList()) {
      var copy = instrCallNode.target().copy();
      for (var pair : instrCallNode.getZippedArgumentsWithParameters().toList()) {
        var isField = pair.left().isLeft();
        var isFieldAccess = pair.left().isRight();
        var isConstant = pair.right() instanceof ConstantNode;

        if (isField && isConstant) {
          var field = pair.left().left();
          var affectedNodes = copy.behavior().getNodes(FieldRefNode.class)
              .filter(x -> x.formatField().equals(field))
              .toList();

          for (var affected : affectedNodes) {
            affected.replaceAndDelete(pair.right());
          }
        } else if (isFieldAccess && isConstant) {
          var fieldAccess = pair.left().right();
          var affectedNodes = copy.behavior().getNodes(FieldAccessRefNode.class)
              .filter(x -> x.fieldAccess().equals(fieldAccess))
              .toList();

          for (var affected : affectedNodes) {
            affected.replaceAndDelete(pair.right());
          }
        }
      }

      uses.addAll(getRegisterUses(copy.behavior()));
      defs.addAll(getRegisterDefs(copy.behavior()));
    }

    return new Info(uses.stream().sorted(Comparator.comparing(RegisterRef::lowerName)).toList(),
        defs.stream().sorted(Comparator.comparing(RegisterRef::lowerName)).toList()
    );
  }

  /**
   * Output of the pass.
   */
  public record Output(Map<Instruction, Info> machineInstructions,
                       Map<CompilerInstruction, Info> compilerInstructions,
                       Map<PseudoInstruction, Info> pseudoInstructions) {

  }

  /**
   * Uses and defs of registers.
   */
  public record Info(List<RegisterRef> uses, List<RegisterRef> defs) {

  }

  /**
   * Get a list of {@link RegisterRef} which are written. It is considered a
   * register definition when a {@link WriteRegTensorNode} with a
   * constant address exists. However, the only registers without any constraints on the
   * register file will be returned. Also program containers are not part of a "Def".
   *
   * @param behavior of the {@link Instruction}.
   */
  private static List<RegisterRef> getRegisterDefs(Graph behavior) {
    var writeRegCandidates = behavior.getNodes(WriteRegTensorNode.class).toList();
    var writeArtificialCandidates =
        behavior.getNodes(WriteArtificialResNode.class)
            .filter(node -> node.resourceDefinition().innerResourceRef() instanceof RegisterTensor)
            .toList();

    return Stream.concat(writeRegCandidates.stream()
                .filter(node -> isRegister(node, node.registerTensor()))
                .map(DetermineRegisterUsesAndDefsPass::map),
            writeArtificialCandidates.stream()
                .filter(node -> isRegister(node,
                    (RegisterTensor) node.resourceDefinition().innerResourceRef())
                    && node.resourceDefinition().readFunction().parameters().length == 0)
                .map(DetermineRegisterUsesAndDefsPass::map)
        )
        .toList();
  }

  private static boolean isRegister(WriteResourceNode node, RegisterTensor tensor) {
    var allAddressesConstant = node.indices().stream()
        .allMatch(ExpressionNode::isConstant);
    var noConstraints = tensor.constraints().isEmpty();
    var noPc = true;

    if (node instanceof WriteRegTensorNode tensorNode) {
      noPc = !tensorNode.isPcAccess();
    }

    return allAddressesConstant && noConstraints && noPc;
  }

  private static RegisterRef map(WriteRegTensorNode node) {
    var reg = node.registerTensor();
    reg.ensure(reg.indexDimensions().size() < 2,
        "Only register and register files supported");
    if (reg.isSingleRegister()) {
      return new RegisterRef(reg);
    } else {
      return new RegisterRef(reg, ((ConstantNode) node.indices().getFirst()).constant());
    }
  }

  private static RegisterRef map(WriteArtificialResNode node) {
    return new RegisterRef(node.resourceDefinition());
  }

  /**
   * Get a list of {@link RegisterRef} which are read. It is considered a
   * register usage when a {@link ReadRegTensorNode} with a
   * constant address exists. However, the only registers without any constraints on the
   * register file will be returned. Also program counters are not part of a "Use".
   *
   * @param behavior of the {@link Instruction}.
   */
  private static List<RegisterRef> getRegisterUses(Graph behavior) {
    var registers = behavior.getNodes(ReadRegTensorNode.class)
        .filter(readRegTensorNode -> readRegTensorNode.regTensor().isSingleRegister())
        .filter(readRegTensorNode -> !readRegTensorNode.isPcAccess())
        .toList();

    var registerFilesWithConstantAddress = behavior.getNodes(ReadRegTensorNode.class)
        .filter(readRegTensorNode -> readRegTensorNode.regTensor().isRegisterFile())
        .filter(ReadResourceNode::hasConstantAddress)
        .toList();

    return Stream.concat(registers.stream(), registerFilesWithConstantAddress.stream())
        .filter(readRegTensorNode -> !readRegTensorNode.hasConstraintForAddress())
        .filter(readRegTensorNode -> readRegTensorNode.indices().size() == 1)
        // Register should not have any constraints. When it does then there is no
        // need that LLVM knows about it because it should not be a dependency.
        .map(readRegTensorNode -> new RegisterRef(readRegTensorNode.regTensor(),
            ((ConstantNode) readRegTensorNode.indices().getFirst()).constant()))
        .toList();
  }
}
