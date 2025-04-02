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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.rtl.ipg.InstructionProgressGraph;
import vadl.rtl.ipg.nodes.InstructionWordSliceNode;
import vadl.rtl.ipg.nodes.IsInstructionNode;
import vadl.rtl.ipg.nodes.OneHotDecodeNode;
import vadl.rtl.ipg.nodes.RtlConditionalReadNode;
import vadl.rtl.ipg.nodes.RtlReadMemNode;
import vadl.rtl.ipg.nodes.RtlWriteMemNode;
import vadl.rtl.ipg.nodes.SelectByInstructionNode;
import vadl.rtl.map.MiaMapping;
import vadl.rtl.utils.RtlSimplificationRules;
import vadl.types.SIntType;
import vadl.types.UIntType;
import vadl.utils.GraphUtils;
import vadl.viam.Definition;
import vadl.viam.Instruction;
import vadl.viam.Resource;
import vadl.viam.Specification;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.SelectNode;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.WriteResourceNode;
import vadl.viam.graph.dependency.ZeroExtendNode;
import vadl.viam.passes.algebraic_simplication.AlgebraicSimplifier;
import vadl.viam.passes.canonicalization.Canonicalizer;

/**
 * Lower instruction progress graph by introducing is-instruction nodes for read/write nodes
 * and select-by-instruction nodes as inputs. This makes the relation between nodes and the
 * instructions they belong to explicit in the graph. This pass also sets name hints for nodes
 * for signal name generation later.
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
    ipg.getNodes(SelectByInstructionNode.class).forEach(select -> {
      // generate expression that selects output based on sets of instructions
      var oneHot = select.instructions().stream()
          .map(ins -> ipg.add(new IsInstructionNode(ins), ins))
          .map(ExpressionNode.class::cast).toList();
      added.addAll(oneHot);
      var instructions = ipg.getContext(select).instructions();
      var selection = ipg.add(new OneHotDecodeNode(oneHot), instructions);
      added.add(selection);
      select.setSelection(selection);

      // add MiA mapping to decode
      var context = mapping.ensureDecode();
      context.ipgNodes().addAll(oneHot);
      context.ipgNodes().add(selection);
    });

    // optimize
    Canonicalizer.canonicalize(ipg);
    new AlgebraicSimplifier(RtlSimplificationRules.rules).run(ipg);

    // clean up mapping (deleted nodes, mapping of constant nodes)
    for (MiaMapping.NodeContext context : mapping.contexts().values()) {

      // remove deleted nodes (from optimization)
      context.ipgNodes().removeIf(Node::isDeleted);
      context.fixedIpgNodes().removeIf(Node::isDeleted);

      // add constant nodes only to mapping with usage (remove otherwise)
      ipg.getNodes(ConstantNode.class).forEach(constantNode -> {
        if (constantNode.usages().anyMatch(context.ipgNodes()::contains)) {
          context.ipgNodes().add(constantNode);
        } else {
          context.ipgNodes().remove(constantNode);
          context.fixedIpgNodes().remove(constantNode);
        }
      });
    }

    // set name hints on nodes for variable name generation
    optIsa.get().ownRegisters().forEach(reg -> nameReadWrite(ipg, reg));
    optIsa.get().ownRegisterFiles().forEach(rf -> nameReadWrite(ipg, rf));
    optIsa.get().ownMemories().forEach(mem -> nameReadWrite(ipg, mem));
    name(ipg, ConstantNode.class, this::constName);
    names(ipg, InstructionWordSliceNode.class, this::nameInsWordSlice);
    name(ipg, IsInstructionNode.class, node -> "is_" + nameInsSet(node.instructions(), ipg));
    names(ipg, SignExtendNode.class,
        node -> ipg.getContext(node.value()).nameHints().stream()
            .map(n -> n + "_sext" + node.type().asDataType().bitWidth()).toList());
    names(ipg, ZeroExtendNode.class,
        node -> ipg.getContext(node.value()).nameHints().stream()
            .map(n -> n + "_zext" + node.type().asDataType().bitWidth()).toList());
    names(ipg, TruncateNode.class,
        node -> ipg.getContext(node.value()).nameHints().stream()
            .map(n -> n + "_trunc" + node.type().asDataType().bitWidth()).toList());
    name(ipg, BuiltInCall.class, node -> {
      var args = node.arguments().stream().map(ipg::getContext)
          .map(context -> context.nameHints().stream().findFirst()).toList();
      if (!args.stream().allMatch(Optional::isPresent)) {
        return null;
      }
      if (args.size() == 2) {
        return args.get(0).orElse("") + "_" + node.builtIn().name().toLowerCase() + "_"
            + args.get(1).orElse("");
      }
      return node.builtIn().name().toLowerCase() + "_" + args.stream().filter(Optional::isPresent)
          .map(Optional::get).collect(Collectors.joining("_"));
    });
    nameSelect(ipg);
    nameSelectByIns(ipg);

    return added;
  }

  private String constName(ConstantNode node) {
    if (node.type() instanceof UIntType || node.type() instanceof SIntType) {
      return node.constant().asVal().decimal();
    }
    var hex = node.constant().asVal().hexadecimal("");
    if (hex.isEmpty()) {
      return hex;
    }
    // contract repeating characters in hex
    var chars = hex.toCharArray();
    var sb = new StringBuilder(hex.substring(0, 1));
    for (int i = 1; i < chars.length; i++) {
      var c1 = chars[i - 1];
      var c2 = chars[i];
      if (c1 != c2) {
        sb.append(c2);
      }
    }
    return sb.toString();
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
      var isIns = ipg.add(new IsInstructionNode(instructions), instructions);
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

  private void name(InstructionProgressGraph ipg, @Nullable Node node, String nameHint) {
    if (node != null) {
      var context = ipg.getContext(node);
      context.nameHints().add(nameHint);
    }
  }

  private <T extends Node> void name(InstructionProgressGraph ipg, Class<T> nodeType,
                                     Function<T, String> name) {
    ipg.getNodes(nodeType).forEach(node -> {
      var nameHint = name.apply(node);
      if (nameHint != null) {
        ipg.getContext(node).nameHints().add(nameHint);
      }
    });
  }

  private <T extends Node> void names(InstructionProgressGraph ipg, Class<T> nodeType,
                                      Function<T, List<String>> name) {
    ipg.getNodes(nodeType).forEach(node -> {
      ipg.getContext(node).nameHints().addAll(name.apply(node));
    });
  }

  private void nameReadWrite(InstructionProgressGraph ipg, Resource resource) {
    var reads = ipg.getNodes(RtlConditionalReadNode.class)
        .filter(read -> read.asReadNode().resourceDefinition().equals(resource))
        .toList();
    for (int i = 0; i < reads.size(); i++) {
      var read = reads.get(i);
      var prefix = "read" + read.asReadNode().resourceDefinition().simpleName() + i;
      name(ipg, read.asReadNode(), prefix + "_result");
      name(ipg, read.condition(), prefix + "_enable");
      if (read.asReadNode().hasAddress()) {
        name(ipg, read.asReadNode().address(), prefix + "_addr");
      }
      if (read instanceof RtlReadMemNode readMem) {
        name(ipg, readMem.words(), prefix + "_words");
      }
    }

    var writes = ipg.getNodes(WriteResourceNode.class)
        .filter(write -> write.resourceDefinition().equals(resource))
        .toList();
    for (int i = 0; i < writes.size(); i++) {
      var write = writes.get(i);
      var prefix = "write" + write.resourceDefinition().simpleName() + i;
      name(ipg, write.value(), prefix + "_value");
      name(ipg, write.condition(), prefix + "_enable");
      if (write.hasAddress()) {
        name(ipg, write.address(), prefix + "_addr");
      }
      if (write instanceof RtlWriteMemNode writeMem) {
        name(ipg, writeMem.words(), prefix + "_words");
      }
    }
  }

  private List<String> nameInsWordSlice(InstructionWordSliceNode node) {
    return node.fields().stream().map(Definition::simpleName).toList();
  }

  private String nameInsSet(Set<Instruction> instructions, InstructionProgressGraph ipg) {
    if (instructions.size() > ipg.instructions().size() / 2) {
      // generate name for complement
      var complement = new HashSet<>(ipg.instructions());
      complement.removeAll(instructions);
      return "not_" + nameInsSet(complement, ipg);
    }
    var names = instructions.stream().map(Definition::simpleName).toList();
    var minLen = names.stream().mapToInt(String::length).min().orElse(0);
    if (names.isEmpty() || minLen == 0) {
      return "none";
    }
    // find the longest common prefix in instruction names
    for (int prefix = minLen; prefix > 0; prefix--) {
      var n1 = names.get(0).substring(0, prefix);
      int finalPrefix = prefix;
      if (names.stream().allMatch(n -> n.substring(0, finalPrefix).equals(n1))) {
        return n1;
      }
    }
    // fallback to concatenation of all instruction names
    return instructions.stream()
        .map(Definition::simpleName).collect(Collectors.joining(""));
  }

  private void nameSelect(InstructionProgressGraph ipg) {
    ipg.getNodes(SelectNode.class).forEach(node -> nameSelect(ipg, node));
  }

  private void nameSelect(InstructionProgressGraph ipg, SelectNode node) {
    var nameHints = ipg.getContext(node).nameHints();
    nameHints.forEach(nameHint -> {
      name(ipg, node.trueCase(), nameHint);
      name(ipg, node.falseCase(), nameHint);
      name(ipg, node.condition(), "sel_" + nameHint);
      node.inputs().forEach(input -> {
        if (input instanceof SelectNode sel) {
          nameSelect(ipg, sel);
        }
      });
    });
  }

  private void nameSelectByIns(InstructionProgressGraph ipg) {
    ipg.getNodes(SelectByInstructionNode.class)
        .forEach(node -> nameSelectByIns(ipg, node));
  }

  private void nameSelectByIns(InstructionProgressGraph ipg, SelectByInstructionNode node) {
    var nameHints = ipg.getContext(node).nameHints();
    nameHints.forEach(nameHint -> {
      name(ipg, node.selection(), "sel_" + nameHint);
      if (node.selection() instanceof SelectByInstructionNode sel) {
        nameSelectByIns(ipg, sel);
      }
      node.values().forEach(value -> {
        name(ipg, value, nameHint);
        if (value instanceof SelectByInstructionNode sel) {
          nameSelectByIns(ipg, sel);
        }
      });
    });
  }

}
