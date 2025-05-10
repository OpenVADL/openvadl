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

import static vadl.types.BuiltInTable.ADD;
import static vadl.types.BuiltInTable.ADDS;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.gcb.passes.relocation.model.CompilerRelocation;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Format;
import vadl.viam.Instruction;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.matching.Matcher;
import vadl.viam.matching.TreeMatcher;
import vadl.viam.matching.impl.AnyChildMatcher;
import vadl.viam.matching.impl.BuiltInMatcher;
import vadl.viam.matching.impl.IsReadRegMatcher;

/**
 * Determines the relocation kind for the immediate operands of machine instructions.
 * If the instruction's behavior adds the immediate to the Program Counter (PC),
 * then the relocation kind for this operand is RELATIVE.
 */
public class DetermineRelocationTypeForFieldPass extends Pass {
  public DetermineRelocationTypeForFieldPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("DetermineRelocationTypeForFieldPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var fieldUsages =
        (IdentifyFieldUsagePass.ImmediateDetectionContainer) passResults.lastResultOf(
            IdentifyFieldUsagePass.class);

    viam.isa().map(InstructionSetArchitecture::ownInstructions).orElse(List.of()).forEach(
        instruction -> {
          var immediateKindMap = new HashMap<Format.Field, CompilerRelocation.Kind>();
          fieldUsages.getImmediates(instruction).forEach(
              immField -> {
                // Determine for every instruction's immediate in the specification the
                // relocation kind.
                var relocationKind = determineRelocationKindByBehavior(instruction, immField);
                immediateKindMap.put(immField, relocationKind);
              }
          );
          instruction.attachExtension(new RelocationKindCtx(immediateKindMap));
        }
    );

    return null;
  }

  private CompilerRelocation.Kind determineRelocationKindByBehavior(
      Instruction instruction, Format.Field field) {
    // First check whether even the PC is read.

    var behavior = instruction.behavior();
    var readsPC = behavior.getNodes(ReadRegTensorNode.class)
        .filter(ReadRegTensorNode::isPcAccess).toList();

    if (readsPC.isEmpty()) {
      // If PC is not read then we can use an absolute relocation.
      return CompilerRelocation.Kind.ABSOLUTE;
    }

    // But, the PC is being read, and we need to check how it is referenced.
    var pcRegister = readsPC.getFirst().regTensor();

    // We say it is RELATIVE when a register is added to the given field in a field access
    // function.
    var matcher = new BuiltInMatcher(List.of(ADD, ADDS), List.of(
        new AnyChildMatcher(new IsReadRegMatcher(pcRegister)),
        new AnyChildMatcher(node ->
            node instanceof FieldAccessRefNode refNode
                && refNode.fieldAccess().fieldRef().equals(field)
        )
    ));

    Set<Matcher> matchers = Set.of(
        matcher,
        matcher.swapOperands()
    );

    var inputRegister = TreeMatcher.matches(
        () -> behavior.getNodes(BuiltInCall.class).map(x -> x),
        matchers
    );

    return !inputRegister.isEmpty()
        ? CompilerRelocation.Kind.RELATIVE
        : CompilerRelocation.Kind.ABSOLUTE;
  }
}
