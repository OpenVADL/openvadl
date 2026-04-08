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

import com.google.common.collect.Streams;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.rtl.ipg.InstructionProgressGraph;
import vadl.rtl.ipg.nodes.RtlConditionalReadNode;
import vadl.rtl.ipg.nodes.RtlInstructionWordSliceNode;
import vadl.rtl.ipg.nodes.RtlReadMemNode;
import vadl.rtl.ipg.nodes.RtlReadRegTensorNode;
import vadl.rtl.ipg.nodes.RtlWriteMemNode;
import vadl.rtl.ipg.nodes.RtlWriteRegTensorNode;
import vadl.rtl.utils.GraphMergeUtils;
import vadl.rtl.utils.RtlSimplificationRules;
import vadl.types.BitsType;
import vadl.types.Type;
import vadl.types.UIntType;
import vadl.utils.GraphUtils;
import vadl.viam.Constant;
import vadl.viam.Instruction;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.MicroArchitecture;
import vadl.viam.RegisterResource;
import vadl.viam.RegisterTensor;
import vadl.viam.Specification;
import vadl.viam.ViamError;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.AbstractEndNode;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.ProcCallNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;
import vadl.viam.graph.dependency.ZeroExtendNode;
import vadl.viam.passes.algebraic_simplication.AlgebraicSimplifier;
import vadl.viam.passes.canonicalization.Canonicalizer;

/**
 * Create the instruction progress graph from instruction behaviors. Adds the result to the
 * instruction set architecture definition as an extension.
 *
 * <p>The creation includes some steps after combining all instruction behaviors into one:
 * <li> replace read/write nodes with RTL-specific implementations
 * <li> modify register file reads/writes to implement register file constraints in the instruction
 * behavior (constant values for specific register addresses)
 * <li> canonicalize and optimize to remove potentially unnecessary constant nodes
 */
public class InstructionProgressGraphCreationPass extends Pass {

  public InstructionProgressGraphCreationPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("Instruction Progress Graph Creation");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var optIsa = viam.mia().map(MicroArchitecture::isa);
    if (optIsa.isEmpty()) {
      return null;
    }

    // copy all instruction behavior into one graph
    var ipg = new InstructionProgressGraph("Instruction Progress Graph", optIsa.get());
    optIsa.get().ownInstructions().forEach(instr -> processInstruction(ipg, instr));

    // replace read/write nodes with implementations suiting the RTL
    adaptReadWriteNodes(optIsa.get(), ipg);
    setReadConditions(ipg);

    // modify register file reads/writes to be disabled and constants to be returned
    // if a constrained addresses is matched
    inlineRegisterFileConstraints(ipg);

    // create instruction fetch and pc increment nodes
    inlineInstructionFetch(ipg, optIsa.get());

    // optimize because some of the previous steps introduce
    // potentially unnecessary constant nodes
    Canonicalizer.canonicalize(ipg);
    new AlgebraicSimplifier(RtlSimplificationRules.rules).run(ipg);

    // Attach IPG to ISA
    var ipgExt = new InstructionProgressGraphExtension(ipg);
    optIsa.get().attachExtension(ipgExt);

    return ipg;
  }

  private void processInstruction(InstructionProgressGraph ipg, Instruction instruction) {

    // replace FieldRefNodes with InstructionWordSliceNodes
    // instruction word slice nodes are unique across formats (are added only once to the IPG) and
    // collect the fields referencing the same slice in a set
    var behavior = instruction.behavior().copy();
    behavior.getNodes(FieldRefNode.class).toList().forEach(fieldRefNode -> {
      var field = fieldRefNode.formatField();
      var slice = behavior.addWithInputs(
          new RtlInstructionWordSliceNode(field.format().type(), field.bitSlice(), field.type()));
      slice.addField(field);
      fieldRefNode.replaceAndDelete(slice);
    });

    // inline procedure calls (exceptions)
    inlineProcCalls(behavior);

    // merge non-concurrent writes since we discard control flow nodes in the next step
    GraphMergeUtils.mergeWritesOnBranches(behavior);

    // copy side effect nodes to IPG
    behavior.getNodes(SideEffectNode.class).forEach(sideEffect -> {
      var copy = sideEffect.copy();
      ipg.addWithInputs(copy, Collections.singleton(instruction));
    });
  }

  private void adaptReadWriteNodes(InstructionSetArchitecture isa, InstructionProgressGraph ipg) {
    // replace register tensor reads
    isa.registerTensors().forEach(reg -> {
      ipg.getNodes(ReadRegTensorNode.class)
          .filter(read -> read.resourceDefinition().equals(reg))
          .toList().forEach(read -> {
            ipg.replaceAndDelete(read, new RtlReadRegTensorNode(reg, read.indices(),
                read.type().asDataType().toBitsType(), GraphUtils.bool(true).toNode(),
                read.staticCounterAccess()));
          });
    });
    // replace register tensor writes
    isa.registerTensors().forEach(reg -> {
      ipg.getNodes(WriteRegTensorNode.class)
          .filter(write -> write.resourceDefinition().equals(reg))
          .toList().forEach(write -> {
            ipg.replaceAndDelete(write, new RtlWriteRegTensorNode(reg, write.indices(),
                write.value(), write.staticCounterAccess(), write.condition()));
          });
    });

    isa.ownMemories().forEach(mem -> {
      // replace memory reads
      var maxRead = ipg.getNodes(ReadMemNode.class)
          .filter(read -> read.resourceDefinition().equals(mem))
          .mapToInt(ReadMemNode::words).max();
      if (maxRead.isPresent()) {
        var wordType = UIntType.minimalTypeFor(maxRead.getAsInt());
        var resType = Type.bits(mem.resultType().bitWidth() * maxRead.getAsInt());
        ipg.getNodes(ReadMemNode.class)
            .filter(read -> read.resourceDefinition().equals(mem))
            .toList().forEach(read -> {
              var words = new ConstantNode(Constant.Value.of(read.words(), wordType));
              ExpressionNode rtlRead = new RtlReadMemNode(read.memory(), maxRead.getAsInt(), words,
                  read.address(), resType, GraphUtils.bool(true).toNode());
              if (read.type().bitWidth() < resType.bitWidth()) {
                rtlRead = new TruncateNode(rtlRead, read.type());
              }
              ipg.replaceAndDelete(read, rtlRead);
            });
      }

      // replace memory writes
      var maxWrite = ipg.getNodes(WriteMemNode.class)
          .filter(write -> write.resourceDefinition().equals(mem))
          .mapToInt(WriteMemNode::words).max();
      if (maxWrite.isPresent()) {
        var wordType = UIntType.minimalTypeFor(maxWrite.getAsInt());
        var valType = Type.bits(mem.resultType().bitWidth() * maxWrite.getAsInt());
        ipg.getNodes(WriteMemNode.class)
            .filter(write -> write.resourceDefinition().equals(mem))
            .toList().forEach(write -> {
              ExpressionNode value = write.value();
              while (value instanceof TruncateNode truncate) {
                value = truncate.value();
              }
              if (value.type().asDataType().bitWidth() < valType.bitWidth()) {
                value = new ZeroExtendNode(value, valType);
              }
              if (value.type().asDataType().bitWidth() > valType.bitWidth()) {
                value = new TruncateNode(value, valType);
              }
              var words = new ConstantNode(Constant.Value.of(write.words(), wordType));
              var rtlWrite = new RtlWriteMemNode(write.memory(), maxWrite.getAsInt(), words,
                  write.address(), value, write.nullableCondition());
              ipg.replaceAndDelete(write, rtlWrite);
            });
      }
    });
  }

  private void setReadConditions(InstructionProgressGraph ipg) {
    var map = new LinkedHashMap<RtlConditionalReadNode, Set<ExpressionNode>>();
    ipg.getNodes(SideEffectNode.class).forEach(sideEffect -> {
      collectSideEffects(sideEffect, sideEffect, map);
    });
    map.forEach((read, sideEffects) -> {
      var instructions = ipg.getContext(read.asReadNode()).instructions();
      if (sideEffects.contains(null) || sideEffects.stream()
          .anyMatch(e -> e instanceof ConstantNode c && c.constant().asVal().bool())) {
        // condition is true
        read.setCondition(
            ipg.addWithInputs(GraphUtils.bool(true).toNode(), instructions));
      } else {
        sideEffects.stream()
            .reduce(GraphUtils::or).ifPresent(cond -> {
              if (hasCycle(read.asReadNode(), cond)) {
                // over-approximate with true
                read.setCondition(
                    ipg.addWithInputs(GraphUtils.bool(true).toNode(), instructions));
              } else {
                if (read.condition() != null) {
                  cond = GraphUtils.and(cond, read.condition());
                }
                cond = ipg.addWithInputs(cond, instructions);
                read.setCondition(cond);
              }
            });
      }
    });
  }

  private void collectSideEffects(Node node, SideEffectNode sideEffect,
                                  Map<RtlConditionalReadNode, Set<ExpressionNode>> map) {
    if (node instanceof RtlConditionalReadNode read) {
      map.computeIfAbsent(read, k -> new LinkedHashSet<>())
          .add(sideEffect.condition());
    }
    node.inputs().forEach(input -> collectSideEffects(input, sideEffect, map));
  }

  // search inputs recursively for node search to find a potential cycle
  // before adding new conditions to reads
  private boolean hasCycle(Node search, Node node) {
    if (search.equals(node)) {
      return true;
    }
    return node.inputs().anyMatch(input -> hasCycle(search, input));
  }

  private void inlineRegisterFileConstraints(InstructionProgressGraph ipg) {
    ipg.getNodes(RtlReadRegTensorNode.class).toList().forEach(read -> {
      ExpressionNode result = read;
      ExpressionNode cond = read.condition();
      for (RegisterResource.Constraint constraint : read.registerTensor().constraints()) {
        if (constraint.indices().isEmpty()) {
          continue;
        }

        // node for: one of the indices does not match the constraint
        var neq = newIndicesNeq(read.indices(), constraint.indices());

        // patch result and condition
        result = GraphUtils.select(neq, result, constraint.value().toNode());
        if (cond == null) {
          cond = neq;
        } else {
          cond = GraphUtils.and(cond, neq);
        }
      }
      if (cond != null && !cond.isActive()) {
        cond = ipg.addWithInputs(cond, ipg.getContext(read).instructions());
      }
      if (result != read) {
        ipg.replace(read, result);
      }
      if (cond != null) {
        read.setCondition(cond);
      }
    });
    ipg.getNodes(WriteRegTensorNode.class).toList().forEach(write -> {
      var instructions = ipg.getContext(write).instructions();
      var cond = write.condition();
      for (RegisterResource.Constraint constraint : write.registerTensor().constraints()) {
        if (constraint.indices().isEmpty()) {
          continue;
        }

        cond = GraphUtils.and(
            cond,
            newIndicesNeq(write.indices(), constraint.indices())
        );
      }
      if (cond != write.condition()) {
        cond = ipg.addWithInputs(cond, instructions);
        write.setCondition(cond);
      }
    });
  }

  private ExpressionNode newIndicesNeq(NodeList<ExpressionNode> indices,
                               List<Constant.Value> constrValues) {
    return Streams.zip(
            indices.stream(), constrValues.stream(),
            (index, constr) -> GraphUtils.neq(index, constr.toNode())
        )
        .reduce(GraphUtils::or).orElseThrow(() ->
            new ViamError("Malformed register file constraint without indices"));
  }

  private void inlineInstructionFetch(InstructionProgressGraph ipg,
                                      InstructionSetArchitecture isa) {
    var codeMem = isa.codeMemory();
    var minInsWord = isa.ownFormats().stream().mapToInt(f -> f.type().bitWidth()).min();
    var maxInsWord = isa.ownFormats().stream().mapToInt(f -> f.type().bitWidth()).max();
    var pc = isa.pc();
    if (pc == null || minInsWord.isEmpty() || maxInsWord.isEmpty()
        || minInsWord.getAsInt() != maxInsWord.getAsInt()) {
      throw new ViamError("Can only fetch instructions with a single instruction width");
    }
    if ((maxInsWord.getAsInt() % codeMem.resultType().bitWidth()) != 0) {
      throw new ViamError("Instruction word length must be a multiple of code memory result width");
    }
    var insWord = maxInsWord.getAsInt();
    var pcInc = maxInsWord.getAsInt() / codeMem.resultType().bitWidth();
    var pcIncVal = Constant.Value
        .of(pcInc, Type.bits(BitsType.minimalRequiredWidthFor(pcInc))).asVal();

    // read pc address
    var readPc = new RtlReadRegTensorNode(pc.registerTensor(), new NodeList<>(),
        pc.resultType(), Constant.Value.of(true).toNode(), pc);
    readPc = ipg.addWithInputs(readPc, ipg.instructions());
    ipg.setPcRead(readPc);

    // read instruction at pc
    var readIns = new RtlReadMemNode(codeMem, pcInc, pcIncVal.toNode(), readPc,
        Type.bits(insWord), Constant.Value.of(true).toNode());
    readIns = ipg.addWithInputs(readIns, ipg.instructions());
    ipg.setFetch(readIns);

    // pc increment, TODO: replace with branch prediction value
    var pcIncRes = GraphUtils.add(
        readPc,
        Constant.Value.of(pcInc, pc.resultType()).asVal().toNode()
    );
    var writePc = new RtlWriteRegTensorNode(pc.registerTensor(), new NodeList<>(),
        pcIncRes, pc, Constant.Value.of(true).toNode());
    writePc = ipg.addWithInputs(writePc, ipg.instructions());
    ipg.setPcIncrement(writePc);

    // disable other pc writes if they write the same value
    // (rollback only considers write enable on other pc writes, would cause unnecessary rollback)
    ipg.getNodes(WriteRegTensorNode.class).toList().forEach(write -> {
      if (!write.equals(ipg.pcIncrement())
          && write.resourceDefinition().equals(pc.registerTensor())) {
        var cond = GraphUtils.and(write.condition(), GraphUtils.neq(pcIncRes, write.value()));
        cond = ipg.addWithInputs(cond, ipg.instructions());
        write.setCondition(cond);
      }
    });
  }

  private void inlineProcCalls(Graph behavior) {
    List<ProcCallNode> procCalls;
    while (true) {
      procCalls = behavior.getNodes(ProcCallNode.class).toList();
      if (procCalls.isEmpty()) {
        break;
      }

      for (ProcCallNode procCall : procCalls) {
        var procBehavior = procCall.procedure().behavior().copy();
        var paramNodes = procBehavior.getNodes(FuncParamNode.class).toList();

        Streams.forEachPair(
            procCall.arguments().stream(),
            Arrays.stream(procCall.procedure().parameters()),
            (arg, param) -> {

              // replace parameter nodes with arguments
              for (var paramNode : paramNodes) {
                if (paramNode.parameter() == param) {
                  var argCopy = Objects.requireNonNull(arg).copy();
                  paramNode.replaceAndDelete(argCopy);
                }
              }
            });

        // copy side effects to behavior and remove procedure call
        // patch side effects with procedure call condition
        procBehavior.getNodes(SideEffectNode.class).forEach(sideEffect -> {
          sideEffect = behavior.addWithInputs(sideEffect.copy(SideEffectNode.class));
          if (procCall.nullableCondition() != null) {
            var cond = behavior.add(GraphUtils.and(sideEffect.condition(), procCall.condition()));
            sideEffect.setCondition(cond);
          }
        });
        for (Node usage : procCall.usages().toList()) {
          if (usage instanceof AbstractEndNode end) {
            end.removeSideEffect(procCall);
          }
        }
        procCall.safeDelete();
      }
    }
  }
}
