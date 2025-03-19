package vadl.rtl.passes;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.rtl.ipg.InstructionProgressGraph;
import vadl.rtl.ipg.nodes.InstructionWordSliceNode;
import vadl.rtl.ipg.nodes.RtlConditionalReadNode;
import vadl.rtl.ipg.nodes.RtlReadMemNode;
import vadl.rtl.ipg.nodes.RtlReadRegFileNode;
import vadl.rtl.ipg.nodes.RtlReadRegNode;
import vadl.rtl.ipg.nodes.RtlWriteMemNode;
import vadl.types.Type;
import vadl.types.UIntType;
import vadl.utils.GraphUtils;
import vadl.viam.Constant;
import vadl.viam.Instruction;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.RegisterFile;
import vadl.viam.Specification;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.ReadRegNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegFileNode;
import vadl.viam.graph.dependency.ZeroExtendNode;
import vadl.viam.passes.algebraic_simplication.AlgebraicSimplificationPass;
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
    var optIsa = viam.isa();
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

    // optimize because some of the previous steps introduce
    // potentially unnecessary constant nodes
    Canonicalizer.canonicalize(ipg);
    new AlgebraicSimplifier(AlgebraicSimplificationPass.rules).run(ipg);

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
          new InstructionWordSliceNode(field.format().type(), field.bitSlice(), field.type()));
      slice.addField(field);
      fieldRefNode.replaceAndDelete(slice);
    });

    // copy side effect nodes to IPG
    behavior.getNodes(SideEffectNode.class).forEach(sideEffect -> {
      var copy = sideEffect.copy();
      ipg.addWithInputs(copy, Collections.singleton(instruction));
    });
  }

  private void adaptReadWriteNodes(InstructionSetArchitecture isa, InstructionProgressGraph ipg) {
    // replace register reads
    isa.ownRegisters().forEach(reg -> {
      ipg.getNodes(ReadRegNode.class)
          .filter(read -> read.resourceDefinition().equals(reg))
          .toList().forEach(read -> {
            ipg.replaceAndDelete(read, new RtlReadRegNode(reg, read.type(),
                GraphUtils.bool(true).toNode()));
          });
    });

    // replace register file reads
    isa.ownRegisterFiles().forEach(regFile -> {
      ipg.getNodes(ReadRegFileNode.class)
          .filter(read -> read.resourceDefinition().equals(regFile))
          .toList().forEach(read -> {
            ipg.replaceAndDelete(read,
                new RtlReadRegFileNode(regFile, read.address(), read.type(),
                    GraphUtils.bool(true).toNode()));
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
              ExpressionNode rtlRead = new RtlReadMemNode(read.memory(), words, read.address(),
                  resType, GraphUtils.bool(true).toNode());
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
              var words = new ConstantNode(Constant.Value.of(write.words(), wordType));
              ExpressionNode value = write.value();
              if (write.value().type().asDataType().bitWidth() > valType.bitWidth()) {
                value = new ZeroExtendNode(value, valType);
              }
              var rtlWrite = new RtlWriteMemNode(write.memory(), words, write.address(), value,
                  write.nullableCondition());
              ipg.replaceAndDelete(write, rtlWrite);
            });
      }
    });
  }

  private void setReadConditions(InstructionProgressGraph ipg) {
    var map = new HashMap<RtlConditionalReadNode, Set<ExpressionNode>>();
    ipg.getNodes(SideEffectNode.class).forEach(sideEffect -> {
      collectSideEffects(sideEffect, sideEffect, map);
    });
    map.forEach((read, sideEffects) -> {
      var instructions = ipg.getContext(read.asNode()).instructions();
      sideEffects.stream()
          .reduce(GraphUtils::or).ifPresent(cond -> {
            if (hasCycle(read.asNode(), cond)) {
              // over-approximate with true
              read.setCondition(
                  ipg.addWithInputs(GraphUtils.bool(true).toNode(), instructions));
            } else {
              if (read.condition() == null) {
                if (!cond.isActiveIn(ipg)) {
                  cond = ipg.addWithInputs(cond, instructions);
                }
              } else {
                cond = ipg.addWithInputs(GraphUtils.and(cond, read.condition()), instructions);
              }
              read.setCondition(cond);
            }
          });
    });
  }

  private void collectSideEffects(Node node, SideEffectNode sideEffect,
                                  Map<RtlConditionalReadNode, Set<ExpressionNode>> map) {
    if (node instanceof RtlConditionalReadNode read && read.condition() != null) {
      map.computeIfAbsent(read, k -> new HashSet<>())
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
    ipg.getNodes(RtlReadRegFileNode.class).toList().forEach(read -> {
      ExpressionNode result = read;
      ExpressionNode cond = read.condition();
      for (RegisterFile.Constraint constraint : read.registerFile().constraints()) {
        var constant = new ConstantNode(constraint.address());
        var neq = GraphUtils.neq(read.address(), constant);
        result = GraphUtils.select(neq, result, new ConstantNode(constraint.value()));
        if (cond == null) {
          cond = neq;
        } else {
          cond = GraphUtils.and(cond, neq);
        }
      }
      if (cond != null) {
        cond = ipg.addWithInputs(cond, ipg.getContext(read).instructions());
      }
      if (result != read) {
        ipg.replace(read, result);
      }
      if (cond != null) {
        read.setCondition(cond);
      }
    });
    ipg.getNodes(WriteRegFileNode.class).toList().forEach(write -> {
      var instructions = ipg.getContext(write).instructions();
      var cond = write.condition();
      for (RegisterFile.Constraint constraint : write.registerFile().constraints()) {
        cond = GraphUtils.and(
            cond,
            GraphUtils.neq(write.address(), new ConstantNode(constraint.address()))
        );
      }
      if (cond != write.condition()) {
        cond = ipg.addWithInputs(cond, instructions);
        write.setCondition(cond);
      }
    });
  }
}
