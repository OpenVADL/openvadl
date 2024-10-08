package vadl.lcb.passes.llvmLowering.strategies.visitors.impl;

import static vadl.viam.ViamError.ensure;
import static vadl.viam.ViamError.ensureNonNull;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.error.DeferredDiagnosticStore;
import vadl.error.Diagnostic;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.llvmLowering.LlvmNodeLowerable;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmAddSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmAndSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBasicBlockSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBrCcSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBrCondSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmFieldAccessRefNode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmLoadSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmMulSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmOrSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmReadRegFileNode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmSDivSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmSExtLoad;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmSMulSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmSRemSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmSetccSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmShlSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmShrSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmSraSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmStoreSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmSubSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmTruncStore;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmTypeCastSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmUDivSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmUMulSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmURemSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmXorSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmZExtLoad;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenNodeVisitor;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenPatternLowerable;
import vadl.types.BitsType;
import vadl.types.BuiltInTable;
import vadl.types.DataType;
import vadl.types.SIntType;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.AbstractBeginNode;
import vadl.viam.graph.control.BranchEndNode;
import vadl.viam.graph.control.IfNode;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.graph.control.InstrEndNode;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncCallNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.LetNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.ReadRegNode;
import vadl.viam.graph.dependency.SelectNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegFileNode;
import vadl.viam.graph.dependency.WriteRegNode;
import vadl.viam.graph.dependency.ZeroExtendNode;

/**
 * Replaces VIAM nodes with LLVM nodes which have more
 * information for the lowering.
 */
public class ReplaceWithLlvmSDNodesVisitor
    implements TableGenNodeVisitor, TableGenPatternLowerable {

  private static final Logger logger = LoggerFactory.getLogger(ReplaceWithLlvmSDNodesVisitor.class);
  private boolean patternLowerable = true;

  @Override
  public boolean isPatternLowerable() {
    return this.patternLowerable;
  }

  @Override
  public void visit(Node node) {
    node.accept(this);
  }

  @Override
  public void visit(ConstantNode node) {
    // Upcast it to a higher type because TableGen is not able to cast implicitly.
    var types = node.usages()
        .filter(x -> x instanceof ExpressionNode)
        .map(x -> {
          var y = (ExpressionNode) x;
          // Cast to BitsType when SIntType
          return y.type();
        })
        .filter(x -> x instanceof BitsType)
        .map(x -> (BitsType) x)
        .sorted(Comparator.comparingInt(BitsType::bitWidth))
        .toList();

    var distinctTypes = new HashSet<>(types);

    if (distinctTypes.size() > 1) {
      DeferredDiagnosticStore.add(
          Diagnostic.warning("Constant must be upcasted but it has multiple candidates. "
                  + "The compiler generator considered only the first type as upcast.",
              node.sourceLocation()).build());
    } else if (distinctTypes.isEmpty()) {
      DeferredDiagnosticStore.add(
          Diagnostic.warning("Constant must be upcasted but it has no candidates.",
              node.sourceLocation()).build());
      return;
    }

    var type = types.stream().findFirst().get();
    node.setType(type);
    node.constant().setType(type);
  }

  @Override
  public void visit(BuiltInCall node) {
    if (node instanceof LlvmNodeLowerable) {
      for (var arg : node.arguments()) {
        visit(arg);
      }

      return;
    }

    if (node.builtIn() == BuiltInTable.ADD || node.builtIn() == BuiltInTable.ADDS) {
      node.replaceAndDelete(new LlvmAddSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.SUB) {
      node.replaceAndDelete(new LlvmSubSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.MUL || node.builtIn() == BuiltInTable.MULS) {
      node.replaceAndDelete(new LlvmMulSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.SMULL || node.builtIn() == BuiltInTable.SMULLS) {
      node.replaceAndDelete(new LlvmSMulSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.UMULL || node.builtIn() == BuiltInTable.UMULLS) {
      node.replaceAndDelete(new LlvmUMulSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.SDIV || node.builtIn() == BuiltInTable.SDIVS) {
      node.replaceAndDelete(new LlvmSDivSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.UDIV || node.builtIn() == BuiltInTable.UDIVS) {
      node.replaceAndDelete(new LlvmUDivSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.SMOD || node.builtIn() == BuiltInTable.SMODS) {
      node.replaceAndDelete(new LlvmSRemSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.UMOD || node.builtIn() == BuiltInTable.UMODS) {
      node.replaceAndDelete(new LlvmURemSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.AND || node.builtIn() == BuiltInTable.ANDS) {
      node.replaceAndDelete(new LlvmAndSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.OR || node.builtIn() == BuiltInTable.ORS) {
      node.replaceAndDelete(new LlvmOrSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.XOR || node.builtIn() == BuiltInTable.XORS) {
      node.replaceAndDelete(new LlvmXorSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.LSL || node.builtIn() == BuiltInTable.LSLS) {
      node.replaceAndDelete(new LlvmShlSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.LSR || node.builtIn() == BuiltInTable.LSRS) {
      node.replaceAndDelete(new LlvmShrSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.ASR || node.builtIn() == BuiltInTable.ASRS) {
      node.replaceAndDelete(new LlvmSraSD(node.arguments(), node.type()));
    } else if (LlvmSetccSD.supported.contains(node.builtIn())) {
      var replaced = (LlvmSetccSD) node.replaceAndDelete(
          new LlvmSetccSD(node.builtIn(), node.arguments(), node.type()));
      //def : Pat< ( setcc X:$rs1, 0, SETEQ ),
      //           ( SLTIU X:$rs1, 1 ) >;
      // By adding it as argument, we get the printing of "SETEQ" for free.
      var newArg = new ConstantNode(new Constant.Str(replaced.llvmCondCode().name()));
      ensure(replaced.graph() != null, "graph must exist");
      replaced.arguments().add(replaced.graph().addWithInputs(newArg));
    } else {
      throw Diagnostic.error("Lowering to LLVM was not implemented", node.sourceLocation()).build();
    }

    for (var arg : node.arguments()) {
      visit(arg);
    }
  }

  @Override
  public void visit(WriteRegNode writeRegNode) {
    if (writeRegNode.hasAddress()) {
      visit(Objects.requireNonNull(writeRegNode.address()));
    }
    visit(writeRegNode.value());
  }

  @Override
  public void visit(WriteRegFileNode writeRegFileNode) {
    if (writeRegFileNode.hasAddress()) {
      visit(Objects.requireNonNull(writeRegFileNode.address()));
    }
    visit(writeRegFileNode.value());
  }

  @Override
  public void visit(WriteMemNode writeMemNode) {
    // LLVM has a special selection dag node when the memory
    // is written and the value truncated.
    if (writeMemNode.value() instanceof TruncateNode truncateNode) {
      var node = new LlvmTruncStore(writeMemNode, truncateNode);
      writeMemNode.replaceAndDelete(node);
    } else {
      var node = new LlvmStoreSD(Objects.requireNonNull(writeMemNode.address()),
          writeMemNode.value(),
          writeMemNode.memory(),
          writeMemNode.words());

      writeMemNode.replaceAndDelete(node);
    }
    if (writeMemNode.hasAddress()) {
      visit(Objects.requireNonNull(writeMemNode.address()));
    }
    visit(writeMemNode.value());
  }

  @Override
  public void visit(SliceNode sliceNode) {
    visit(sliceNode.value());
  }

  @Override
  public void visit(SelectNode selectNode) {
    logger.atWarn().log("Conditionals are in the instruction's behavior is not lowerable");
    patternLowerable = false;
  }

  @Override
  public void visit(ReadRegNode readRegNode) {
    if (readRegNode.hasAddress()) {
      visit(readRegNode.address());
    }
  }

  @Override
  public void visit(ReadRegFileNode readRegFileNode) {
    // If the address is constant and register file has a constraint for, then we should replace it
    // by the constraint value.

    if (readRegFileNode.hasConstantAddress()) {
      var address = (ConstantNode) readRegFileNode.address();
      var constraint = Arrays.stream(readRegFileNode.registerFile().constraints())
          .filter(c -> c.address().equals(address.constant()))
          .findFirst();

      if (constraint.isPresent()) {
        var constantNode = new ConstantNode(constraint.get().value());
        readRegFileNode.replaceAndDelete(constantNode);
        visit(constantNode);
      } else {
        DeferredDiagnosticStore.add(Diagnostic.warning(
            "Reading from a register file with constant index but the register has no "
                + "constraint value.",
            address.sourceLocation()).build());
      }
    } else {

      visit(readRegFileNode.address());
      if (readRegFileNode instanceof LlvmReadRegFileNode) {
        return;
      }

      readRegFileNode.replaceAndDelete(
          new LlvmReadRegFileNode(readRegFileNode.registerFile(), readRegFileNode.address(),
              readRegFileNode.type(), readRegFileNode.staticCounterAccess()));
    }
  }

  @Override
  public void visit(ReadMemNode readMemNode) {
    var cast = new LlvmTypeCastSD(new LlvmLoadSD(readMemNode), readMemNode.type());
    readMemNode.replaceAndDelete(cast);
    visit(readMemNode.address());
  }

  @Override
  public void visit(LetNode letNode) {
    letNode.replaceAndDelete(letNode.expression());
    visit(letNode.expression());
  }

  @Override
  public void visit(FuncParamNode funcParamNode) {
  }

  @Override
  public void visit(FuncCallNode funcCallNode) {
    logger.warn("Function calls which are in the instruction's behavior are not lowerable");
    patternLowerable = false;
  }

  @Override
  public void visit(FieldRefNode fieldRefNode) {
  }

  @Override
  public void visit(FieldAccessRefNode fieldAccessRefNode) {
    fieldAccessRefNode.replaceAndDelete(
        new LlvmFieldAccessRefNode(fieldAccessRefNode.fieldAccess(),
            fieldAccessRefNode.type()));
  }

  @Override
  public void visit(AbstractBeginNode abstractBeginNode) {
  }

  @Override
  public void visit(InstrEndNode instrEndNode) {
    for (var arg : instrEndNode.sideEffects()) {
      visit(arg);
    }
  }

  @Override
  public void visit(ReturnNode returnNode) {
    logger.warn("Return node is in the instruction's behavior is not lowerable");
    patternLowerable = false;
  }

  @Override
  public void visit(BranchEndNode branchEndNode) {
    for (var arg : branchEndNode.sideEffects()) {
      visit(arg);
    }
  }

  @Override
  public void visit(InstrCallNode instrCallNode) {
    for (var arg : instrCallNode.arguments()) {
      visit(arg);
    }
  }

  @Override
  public void visit(IfNode ifNode) {
    logger.warn("Conditionals are in the instruction's behavior is not lowerable");
    patternLowerable = false;
  }

  @Override
  public void visit(ZeroExtendNode node) {
    if (node.value() instanceof ReadMemNode readMemNode) {
      // Merge SignExtend and ReadMem to LlvmZExtLoad
      node.replaceAndDelete(
          new LlvmTypeCastSD(new LlvmZExtLoad(readMemNode), makeSigned(node.type())));
      visit(readMemNode.address());
    } else {
      // Remove all nodes
      for (var usage : node.usages().toList()) {
        usage.replaceInput(node, node.value());
      }
      visit(node.value());
    }
  }

  @Override
  public void visit(SignExtendNode node) {
    if (node.value() instanceof ReadMemNode readMemNode) {
      // Merge SignExtend and ReadMem to LlvmSExtLoad
      node.replaceAndDelete(
          new LlvmTypeCastSD(new LlvmSExtLoad(readMemNode), makeSigned(node.type())));
      visit(readMemNode.address());
    } else {
      // Remove all nodes
      for (var usage : node.usages().toList()) {
        usage.replaceInput(node, node.value());
      }
      visit(node.value());
    }
  }

  @Override
  public void visit(TruncateNode node) {
    // Remove all nodes
    for (var usage : node.usages().toList()) {
      usage.replaceInput(node, node.value());
    }
    visit(node.value());
  }

  @Override
  public void visit(ExpressionNode expressionNode) {
    expressionNode.accept(this);
  }

  @Override
  public void visit(SideEffectNode sideEffectNode) {
    sideEffectNode.accept(this);
  }

  @Override
  public void visit(LlvmBrCcSD node) {
    visit(node.first());
    visit(node.second());
    visit(node.immOffset());
  }

  @Override
  public void visit(LlvmFieldAccessRefNode llvmFieldAccessRefNode) {

  }

  @Override
  public void visit(LlvmBrCondSD node) {
    visit(node.condition());
    visit(node.immOffset());
  }

  @Override
  public void visit(LlvmTypeCastSD node) {
    visit(node.value());
  }

  @Override
  public void visit(LlvmTruncStore node) {
    if (node.hasAddress()) {
      visit(Objects.requireNonNull(node.address()));
    }
    if (node.value() != null) {
      visit(node.value());
    }
  }

  @Override
  public void visit(LlvmStoreSD node) {
    if (node.hasAddress()) {
      visit(Objects.requireNonNull(node.address()));
    }
    if (node.value() != null) {
      visit(node.value());
    }
  }

  @Override
  public void visit(LlvmLoadSD node) {
    visit(node.address());
  }

  @Override
  public void visit(LlvmSExtLoad node) {
    visit(node.address());
  }

  @Override
  public void visit(LlvmZExtLoad node) {
    visit(node.address());
  }

  @Override
  public void visit(LlvmSetccSD node) {
    for (var arg : node.arguments()) {
      visit(arg);
    }
  }

  @Override
  public void visit(LlvmBasicBlockSD node) {

  }

  private Type makeSigned(DataType type) {
    if (!type.isSigned()) {
      if (type instanceof BitsType bitsType) {
        return SIntType.bits(bitsType.bitWidth());
      }
    }

    return type;
  }
}
