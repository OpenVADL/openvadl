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

package vadl.rtl.passes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.rtl.ipg.InstructionProgressGraph;
import vadl.rtl.ipg.nodes.RtlConditionalReadNode;
import vadl.rtl.ipg.nodes.RtlDecodeTreeNode;
import vadl.rtl.ipg.nodes.RtlInstructionWordSliceNode;
import vadl.rtl.ipg.nodes.RtlIsInstructionNode;
import vadl.rtl.ipg.nodes.RtlOneHotDecodeNode;
import vadl.rtl.ipg.nodes.RtlSelectByInstructionNode;
import vadl.rtl.map.MiaMapping;
import vadl.rtl.utils.RtlSimplificationRules;
import vadl.rtl.utils.RtlSimplifier;
import vadl.types.UIntType;
import vadl.utils.GraphUtils;
import vadl.viam.Specification;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.WriteResourceNode;

/**
 * Lower instruction progress graph by introducing is-instruction nodes for read/write nodes
 * and select-by-instruction nodes as inputs. This makes the relation between nodes and the
 * instructions they belong to explicit in the graph.
 */
public class InstructionProgressGraphLowerPass extends Pass {

  public InstructionProgressGraphLowerPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("Instruction Progress Graph Lower");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var isa = viam.isa().orElse(null);
    var mia = viam.mia().orElse(null);
    if (isa == null || mia == null) {
      return null;
    }

    var ipg = isa.expectExtension(InstructionProgressGraphExtension.class).ipg();
    var mapping = mia.extension(MiaMapping.class);
    if (mapping == null) {
      return null;
    }

    var decodeContext = mapping.ensureDecode();

    // TODO: Introduce a 'vdt' node, which considers the 'RtlIsInstructionNode'
    //  and 'RtlOneHotDecodeNode' as 'signals' to output.
    //  Add the instruction word as input to the 'vdt' node

    var vdtDecodeNode = new RtlDecodeTreeNode();

    // TODO: In reality the insn context of the decode node should be isa.ownInstruction(), however
    // then the ipg.fetch() node doesn't depend on 'all' possible instructions anymore and receives
    // an IsInstruction condition, which leads to a circular dependency...
    ipg.add(vdtDecodeNode, ipg.instructions());
    decodeContext.ipgNodes().add(vdtDecodeNode);

    var added = new ArrayList<Node>();

    // patch write and read conditions with is-instruction nodes
    ipg.getNodes(WriteResourceNode.class).forEach(write -> {
      added.addAll(patchCondition(write, write.condition(), vdtDecodeNode, ipg, mapping));
    });
    ipg.getNodes(RtlConditionalReadNode.class).forEach(read -> {
      added.addAll(
          patchCondition(read.asReadNode(), read.nullableCondition(), vdtDecodeNode, ipg, mapping));
    });

    // add select-by-instruction selection inputs
    ipg.getNodes(RtlSelectByInstructionNode.class).forEach(select -> {

      if (select.selection() != null) {
        // TODO: Is that even a viable path?
        return;
      }

      // Generate expression that selects output based on sets of instructions
      var instructions = ipg.getContext(select).instructions();

      var oneHotType = UIntType.minimalTypeFor(select.instructions().size() - 1);
      // TODO: Maybe we wan't to attach the selection's instructions to the oneHot
      var selection = ipg.add(new RtlOneHotDecodeNode(oneHotType, vdtDecodeNode), instructions);

      vdtDecodeNode.addSignal(selection);

      added.add(selection);
      select.setSelection(selection);

      // add MiA mapping to decode
      decodeContext.ipgNodes().add(selection);
    });

    // handle undefined instructions
    // TODO undefined instruction behavior from specification
    var anyIns = ipg.add(
        new RtlIsInstructionNode(isa.ownInstructions(), vdtDecodeNode),
        isa.ownInstructions()
    );
    decodeContext.ipgNodes().add(anyIns);

    // TODO: Create a negated version of the IsInstructionNod (or even a dedicated node)
    //var invalidInsn = ipg.add(GraphUtils.not(anyIns), isa.ownInstructions());
    //decodeContext.ipgNodes().add(anyIns);

    vdtDecodeNode.addSignal(anyIns);

    // TODO: Do we still need this?
    ipg.setUnknownInstruction(anyIns);

    // optimize
    new RtlSimplifier(RtlSimplificationRules.rules).run(ipg, mapping);

    // add instruction input to vdt decode node
    vdtDecodeNode.setInstructionWord(ipg.fetch());

    // add instruction input to instruction word slices
    var insSliceList = ipg.getNodes(RtlInstructionWordSliceNode.class).toList();
    for (RtlInstructionWordSliceNode insSlice : insSliceList) {
      insSlice.setInstruction(ipg.fetch());
    }

    // verify ipg
    ipg.verify();

    return added;
  }

  private List<Node> patchCondition(Node node, @Nullable ExpressionNode cond,
                                    RtlDecodeTreeNode decodeTree,
                                    InstructionProgressGraph ipg, MiaMapping mapping) {
    node.ensure(cond != null, "Condition input must be set before we extend it");
    var instructions = ipg.getContext(node).instructions();
    if (!instructions.containsAll(ipg.instructions())) { // TODO: What's this check for exactly; are we aware that we're processing the instruction word fetch here, too?
      // determine mapping context for existing condition
      var condContext = mapping.ensureContext(cond);
      if (cond.isConstant()) {
        condContext = mapping.ensureDecode();
      }

      var isIns = ipg.add(new RtlIsInstructionNode(instructions, decodeTree), instructions);
      decodeTree.addSignal(isIns);

      // if not active in all instructions, patch condition
      var newCond = ipg.add(GraphUtils.and(cond, isIns), instructions);
      node.replaceInput(cond, newCond);

      // add MiA mapping
      var decodeContext = mapping.ensureDecode();
      decodeContext.ipgNodes().add(isIns);
      // TODO: What if condContext < decodeContext? Then the newCond is mapped to the e.g. FETCH
      //       stage, while it's data dependency (isInsn) is only present in a later stage.
      condContext.ipgNodes().add(newCond);

      return List.of(isIns, newCond);
    }
    return Collections.emptyList();
  }

}
