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
import vadl.rtl.ipg.nodes.RtlIsInstructionNode;
import vadl.rtl.ipg.nodes.RtlOneHotDecodeNode;
import vadl.rtl.ipg.nodes.RtlConditionalReadNode;
import vadl.rtl.ipg.nodes.RtlSelectByInstructionNode;
import vadl.rtl.map.MiaMapping;
import vadl.rtl.utils.RtlSimplificationRules;
import vadl.rtl.utils.RtlSimplifier;
import vadl.utils.GraphUtils;
import vadl.viam.Specification;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.WriteResourceNode;
import vadl.viam.passes.canonicalization.Canonicalizer;

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
    var optIsa = viam.isa();
    if (optIsa.isEmpty()) {
      return null;
    }
    var optMia = viam.mia();
    if (optMia.isEmpty()) {
      return null;
    }

    var ipg = optIsa.get().expectExtension(InstructionProgressGraphExtension.class).ipg();
    var mapping = optMia.get().extension(MiaMapping.class);
    if (mapping == null) {
      return null;
    }
    var added = new ArrayList<Node>();

    // patch write and read conditions with is-instruction nodes
    ipg.getNodes(WriteResourceNode.class).forEach(write -> {
      added.addAll(patchCondition(write, write.condition(), ipg, mapping));
    });
    ipg.getNodes(RtlConditionalReadNode.class).forEach(read -> {
      added.addAll(patchCondition(read.asReadNode(), read.condition(), ipg, mapping));
    });

    // add select-by-instruction selection inputs
    ipg.getNodes(RtlSelectByInstructionNode.class).forEach(select -> {
      if (select.selection() == null) {
        // generate expression that selects output based on sets of instructions
        var oneHot = select.instructions().stream()
            .map(ins -> ipg.add(new RtlIsInstructionNode(ins), ins))
            .map(ExpressionNode.class::cast).toList();
        added.addAll(oneHot);
        var instructions = ipg.getContext(select).instructions();
        var selection = ipg.add(new RtlOneHotDecodeNode(oneHot), instructions);
        added.add(selection);
        select.setSelection(selection);

        // add MiA mapping to decode
        var context = mapping.ensureDecode();
        context.ipgNodes().addAll(oneHot);
        context.ipgNodes().add(selection);
      }
    });

    // optimize
    Canonicalizer.canonicalize(ipg);
    new RtlSimplifier(RtlSimplificationRules.rules).run(ipg, mapping);

    return added;
  }

  private List<Node> patchCondition(Node node, @Nullable ExpressionNode cond,
                                    InstructionProgressGraph ipg, MiaMapping mapping) {
    node.ensure(cond != null, "Condition input must be set before we extend it");
    var instructions = ipg.getContext(node).instructions();
    if (!instructions.containsAll(ipg.instructions())) {
      // determine mapping context for existing condition
      var condContext = mapping.ensureContext(cond);
      if (cond.isConstant()) {
        condContext = mapping.ensureDecode();
      }

      // if not active in all instructions, patch condition
      var isIns = ipg.add(new RtlIsInstructionNode(instructions), instructions);
      var newCond = ipg.add(GraphUtils.and(cond, isIns), instructions);
      node.replaceInput(cond, newCond);

      // add MiA mapping
      var decodeContext = mapping.ensureDecode();
      decodeContext.ipgNodes().add(isIns);
      condContext.ipgNodes().add(newCond);

      return List.of(isIns, newCond);
    }
    return Collections.emptyList();
  }

}
