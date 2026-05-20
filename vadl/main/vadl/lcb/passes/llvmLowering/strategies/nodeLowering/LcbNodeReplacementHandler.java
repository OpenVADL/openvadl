// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.lcb.passes.llvmLowering.strategies.nodeLowering;

import static vadl.viam.ViamError.ensure;
import static vadl.viam.ViamError.ensureNonNull;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import vadl.cppCodeGen.CppTypeMap;
import vadl.error.DeferredDiagnosticStore;
import vadl.error.Diagnostic;
import vadl.gcb.valuetypes.ValueType;
import vadl.javaannotations.DispatchFor;
import vadl.javaannotations.Handler;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmAddSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmAndSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBrCcSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBrCondSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBrSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBrindSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmCondCode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmFieldAccessRefNode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmLoadSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmMulSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmOrSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmReadArtificialResourceNode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmReadRegFileNode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmSDivSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmSExtLoad;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmSMulhSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmSRemSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmSetccSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmShlSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmShrSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmSraSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmSubSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmTargetCallSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmTypeCastSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmUDivSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmUMulhSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmURemSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmUnlowerableSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmXorSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmZExtLoad;
import vadl.types.BitsType;
import vadl.types.BuiltInTable;
import vadl.types.DataType;
import vadl.viam.Constant;
import vadl.viam.PrintableInstruction;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.BranchBeginNode;
import vadl.viam.graph.control.BranchEndNode;
import vadl.viam.graph.control.ForallEndNode;
import vadl.viam.graph.control.ForallNode;
import vadl.viam.graph.control.IfNode;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.graph.control.InstrEndNode;
import vadl.viam.graph.control.MergeNode;
import vadl.viam.graph.control.NewLabelNode;
import vadl.viam.graph.control.ProcEndNode;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.control.ScheduledNode;
import vadl.viam.graph.control.StageEndNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.AsmBuiltInCall;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FoldNode;
import vadl.viam.graph.dependency.ForIdxNode;
import vadl.viam.graph.dependency.FuncCallNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.LabelNode;
import vadl.viam.graph.dependency.LetNode;
import vadl.viam.graph.dependency.OperationExistsNode;
import vadl.viam.graph.dependency.OperationForAllNode;
import vadl.viam.graph.dependency.ProcCallNode;
import vadl.viam.graph.dependency.ReadArtificialResNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.ReadStageOutputNode;
import vadl.viam.graph.dependency.SelectNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.StructGetFieldNode;
import vadl.viam.graph.dependency.TensorNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.UnaryNode;
import vadl.viam.graph.dependency.WriteArtificialResNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;
import vadl.viam.graph.dependency.ZeroExtendNode;

/**
 * Replaces VIAM nodes by LLVM nodes.
 */
@DispatchFor(
    value = Node.class,
    include = {"vadl.viam", "vadl.lcb.passes.llvmLowering.domain.selectionDag"}
)
public class LcbNodeReplacementHandler {
  protected final PrintableInstruction printableInstruction;
  protected final ValueType architectureType;
  protected final ValueType smallestRegisterClassType;

  @SuppressWarnings("MissingJavadocMethod")
  public LcbNodeReplacementHandler(PrintableInstruction printableInstruction,
                                   ValueType architectureType,
                                   ValueType smallestRegisterClassType) {
    this.printableInstruction = printableInstruction;
    this.architectureType = architectureType;
    this.smallestRegisterClassType = smallestRegisterClassType;
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(MergeNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(FieldRefNode node) {
    // do nothing because we do not replace anything
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(SideEffectNode sideEffectNode) {
    throw Diagnostic.error("not handled", sideEffectNode.location()).build();
  }

  @Handler
  public void handle(ProcCallNode node) {
    Objects.requireNonNull(node.graph()).add(new LlvmUnlowerableSD());
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(TensorNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(ProcEndNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(ForallNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(ForallEndNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(ReadArtificialResNode node) {
    if (node.indices().isEmpty()) {
      Objects.requireNonNull(node.graph()).add(new LlvmUnlowerableSD());
    } else {
      node.replaceAndDelete(
          new LlvmReadArtificialResourceNode(node.resourceDefinition(),
              node.indices().getFirst(),
              node.type()));
    }
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(NewLabelNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(ReadStageOutputNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(FoldNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(OperationForAllNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(OperationExistsNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(LlvmTypeCastSD node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(AsmBuiltInCall node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(StartNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(LlvmBrCondSD node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(ForIdxNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(ScheduledNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(LlvmBrSD node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(LlvmBrindSD node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(BranchBeginNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(StructGetFieldNode node) {
    Objects.requireNonNull(node.graph()).add(new LlvmUnlowerableSD());
    LcbNodeReplacementHandlerDispatcher.dispatch(this, node.expression());
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(LlvmBrCcSD node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(FuncParamNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(LlvmTargetCallSD node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(LabelNode node) {
    throw Diagnostic.error("not supported", node.location()).build();
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(BranchEndNode branchEndNode) {
    for (var arg : branchEndNode.sideEffects()) {
      LcbNodeReplacementHandlerDispatcher.dispatch(this, arg);
    }
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(BuiltInCall node) {
    for (var arg : node.arguments()) {
      LcbNodeReplacementHandlerDispatcher.dispatch(this, arg);
    }

    if (node.builtIn() == BuiltInTable.ADD || node.builtIn() == BuiltInTable.ADDS) {
      node.replaceAndDelete(new LlvmAddSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.SUB) {
      node.replaceAndDelete(new LlvmSubSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.MUL || node.builtIn() == BuiltInTable.MULS) {
      node.replaceAndDelete(new LlvmMulSD(node.arguments(), node.type()));
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
    } else if ((node.builtIn() == BuiltInTable.SMULL || node.builtIn() == BuiltInTable.SMULLS)
        && node.type() instanceof BitsType bitsType) {
      var trunc = bitsType.bitWidth() / 2;

      // Only replace when parent is a truncate node to the half bit width.
      var truncNode = node.usages().findFirst().filter(x -> x instanceof TruncateNode y
          && y.type().bitWidth() == trunc);
      truncNode.ifPresent(value -> value
          .replaceAndDelete(
              new LlvmMulSD(node.arguments(), ((TruncateNode) value).type())));
    } else if (node.builtIn() == BuiltInTable.SMULL || node.builtIn() == BuiltInTable.SMULLS) {
      /*
        `MUL` and `SMUL` need to be covered in the normal BuiltinReplacement.
        The reason why we are using `BuiltInTable.SMULL, BuiltInTable.SMULLS` is that the "normal"
        multiplication requires two nodes: arithmetic and slice / truncate node.
       */
      if (node.usages().allMatch(usage -> usage instanceof TruncateNode)
          || node.arguments().stream().allMatch(arg -> arg instanceof TruncateNode)) {
        node.replaceAndDelete(new LlvmSMulhSD(node.arguments(), node.type()));
      }
    } else if (LlvmSetccSD.supported.contains(node.builtIn())) {
      for (var arg : node.arguments()) {
        LcbNodeReplacementHandlerDispatcher.dispatch(this, arg);
      }

      // Check if the builtin is used in the condition of a write.
      // If yes, then we do not want this transformation.
      var usages = node.usages().filter(x -> x instanceof WriteRegTensorNode)
          .map(x -> (WriteRegTensorNode) x).toList();
      if (usages.stream().anyMatch(usage -> usage.condition() == node)) {
        return;
      }

      var replaced = node.replaceAndDelete(
          new LlvmSetccSD(node.builtIn(), node.arguments(), node.type()));
      //def : Pat< ( setcc X:$rs1, 0, SETEQ ),
      //           ( SLTIU X:$rs1, 1 ) >;
      // By adding it as argument, we get the printing of "SETEQ" for free.
      var newArg = new ConstantNode(new Constant.Str(replaced.llvmCondCode().name()));
      ensure(replaced.graph() != null, "graph must exist");
      replaced.arguments().add(replaced.graph().addWithInputs(newArg));

      // setcc must not be wrapped by a zext.
      var unary =
          replaced.usages().filter(x -> x instanceof SignExtendNode || x instanceof ZeroExtendNode)
              .map(x -> (UnaryNode) x)
              .toList();

      for (var x : unary) {
        x.replaceAndDelete(x.value());
      }
    } else {
      Objects.requireNonNull(node.graph())
          .add(new LlvmUnlowerableSD(node.arguments(), node.type()));
    }
  }

  /**
   * This method looks at the usages of the given {@code node} and updates the type
   * based on the type of the usage. This is necessary because TableGen cannot cast implicitly.
   */
  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(ConstantNode node) {
    var types = node.usages()
        .filter(x -> x instanceof ExpressionNode)
        .map(x -> {
          var y = (ExpressionNode) x;
          // Cast to BitsType when SIntType
          return y.type();
        })
        .filter(x -> x instanceof DataType)
        .map(x -> (DataType) x)
        .sorted(Comparator.comparingInt(DataType::bitWidth))
        .toList();

    var distinctTypes = new HashSet<>(types);

    if (distinctTypes.size() > 1) {
      DeferredDiagnosticStore.add(
          Diagnostic.warning("Constant must be upcasted but it has multiple candidates. "
                  + "The compiler generator considered only the first type as upcast.",
              node.location()).build());
    } else if (distinctTypes.isEmpty()) {
      DeferredDiagnosticStore.add(
          Diagnostic.warning("Constant must be upcasted but it has no candidates.",
              node.location()).build());
    } else {
      var type = types.stream().findFirst().get();
      node.setType(type);
      node.constant().setType(type);
    }
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(FieldAccessRefNode fieldAccessRefNode) {
    var originalType = fieldAccessRefNode.fieldAccess().accessFunction().returnType();
    var llvmType = ValueType.from(CppTypeMap.upcast(originalType)).orElseThrow(() ->
        Diagnostic.error("Cannot construct LLVM type", fieldAccessRefNode.location()).build());

    llvmType = llvmType.getBitwidth() < this.smallestRegisterClassType.getBitwidth()
        ? this.smallestRegisterClassType
        : llvmType;

    fieldAccessRefNode.replaceAndDelete(
        new LlvmFieldAccessRefNode(
            printableInstruction,
            fieldAccessRefNode.fieldAccess(),
            originalType,
            llvmType,
            LlvmFieldAccessRefNode.Usage.Immediate));
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(FuncCallNode selectNode) {
    if (selectNode.graph() != null) {
      selectNode.graph().add(new LlvmUnlowerableSD());
    }
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(IfNode selectNode) {
    if (selectNode.graph() != null) {
      selectNode.graph().add(new LlvmUnlowerableSD());
    }
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(InstrCallNode instrCallNode) {
    for (var arg : instrCallNode.arguments()) {
      LcbNodeReplacementHandlerDispatcher.dispatch(this, arg);
    }
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(InstrEndNode instrEndNode) {
    for (var arg : instrEndNode.sideEffects()) {
      LcbNodeReplacementHandlerDispatcher.dispatch(this, arg);
    }
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(StageEndNode stageEndNode) {
    for (var arg : stageEndNode.sideEffects()) {
      LcbNodeReplacementHandlerDispatcher.dispatch(this, arg);
    }
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(LetNode node) {
    LcbNodeReplacementHandlerDispatcher.dispatch(this, node.expression());
    node.replaceAndDelete(node.expression());
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(SliceNode sliceNode) {
    if (sliceNode.value() instanceof BuiltInCall bc) {
      /*
        Example: `ty` is `int128`
        then `high` is `128` and `64`.
        A SliceNode requires the bounds `lsb` = `64` and `msb` = `127`.
       */
      var ty = (BitsType) bc.type();
      var high = ty.bitWidth();
      var low = high / 2;

      if (sliceNode.bitSlice().lsb() == low
          && sliceNode.bitSlice().msb() == high - 1) {
        var node = (BuiltInCall) sliceNode.value();
        for (var arg : node.arguments()) {
          LcbNodeReplacementHandlerDispatcher.dispatch(this, arg);
        }

        if (bc.builtIn() == BuiltInTable.SMULL
            || bc.builtIn() == BuiltInTable.SMULLS) {
          sliceNode.replaceAndDelete(new LlvmSMulhSD(node.arguments(), node.type()));
        } else if (bc.builtIn() == BuiltInTable.UMULL || bc.builtIn() == BuiltInTable.UMULLS) {
          sliceNode.replaceAndDelete(new LlvmUMulhSD(node.arguments(), node.type()));
        }
      }
    } else {
      LcbNodeReplacementHandlerDispatcher.dispatch(this, sliceNode.value());
      Objects.requireNonNull(sliceNode.graph()).add(new LlvmUnlowerableSD());
    }
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(ReadMemNode readMemNode) {
    LcbNodeReplacementHandlerDispatcher.dispatch(this, readMemNode.address());
    readMemNode.replaceAndDelete(new LlvmLoadSD(readMemNode));
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(ReadRegTensorNode readRegTensorNode) {
    if (readRegTensorNode.isDeleted()) {
      return;
    }

    if (readRegTensorNode.hasRegisterFile()) {
      // If the address is constant and register file has a constraint for, then we should replace
      // it by the constraint value.

      readRegTensorNode.regTensor()
          .ensure(readRegTensorNode.regTensor().isRegisterFile(), "must be register file");

      if (readRegTensorNode.hasConstantAddress()) {
        var address = (ConstantNode) readRegTensorNode.address();
        var constraint = readRegTensorNode.regTensor().constraints().stream()
            .filter(c -> c.indices().getFirst().equals(address.constant()))
            .findFirst();

        if (constraint.isPresent()) {
          readRegTensorNode.replaceAndDelete(new ConstantNode(constraint.get().value()));
        } else {
          DeferredDiagnosticStore.add(Diagnostic.warning(
              "Reading from a register file with constant index but the register has no "
                  + "constraint value.",
              address.location()).build());
        }
      } else {
        LcbNodeReplacementHandlerDispatcher.dispatch(this, readRegTensorNode.address());

        readRegTensorNode.replaceAndDelete(
            new LlvmReadRegFileNode(readRegTensorNode.regTensor(), readRegTensorNode.address(),
                readRegTensorNode.type(), readRegTensorNode.staticCounterAccess()));
      }
    } else if (readRegTensorNode.regTensor().isSingleRegister()) {
      for (var index : readRegTensorNode.indices()) {
        LcbNodeReplacementHandlerDispatcher.dispatch(this, index);
      }
    }
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(ReturnNode selectNode) {
    if (selectNode.graph() != null) {
      selectNode.graph().add(new LlvmUnlowerableSD());
    }
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(SelectNode selectNode) {
    LcbNodeReplacementHandlerDispatcher.dispatch(this, selectNode.condition());
    LcbNodeReplacementHandlerDispatcher.dispatch(this, selectNode.trueCase());
    LcbNodeReplacementHandlerDispatcher.dispatch(this, selectNode.falseCase());
    if (selectNode.graph() != null) {
      selectNode.graph().add(new LlvmUnlowerableSD());
    }
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(SignExtendNode signExtendNode) {
    if (signExtendNode.value() instanceof ReadMemNode readMemNode) {
      // Merge SignExtend and ReadMem to LlvmSExtLoad
      signExtendNode.replaceAndDelete(new LlvmSExtLoad(readMemNode));
      LcbNodeReplacementHandlerDispatcher.dispatch(this, readMemNode.address());
    } else {
      LcbNodeReplacementHandlerDispatcher.dispatch(this, signExtendNode.value());
    }
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(TruncateNode truncateNode) {
    Objects.requireNonNull(truncateNode.graph()).add(new LlvmUnlowerableSD());
    LcbNodeReplacementHandlerDispatcher.dispatch(this, truncateNode.value());
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(WriteMemNode writeMemNode) {
    Objects.requireNonNull(writeMemNode.graph()).add(new LlvmUnlowerableSD());

    for (var index : writeMemNode.indices()) {
      LcbNodeReplacementHandlerDispatcher.dispatch(this, index);
    }

    LcbNodeReplacementHandlerDispatcher.dispatch(this, writeMemNode.value());
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(WriteArtificialResNode writeArtificialResNode) {
    if (writeArtificialResNode.isDeleted()) {
      return;
    }

    for (var index : writeArtificialResNode.indices()) {
      LcbNodeReplacementHandlerDispatcher.dispatch(this, index);
    }

    LcbNodeReplacementHandlerDispatcher.dispatch(this, writeArtificialResNode.condition());
    LcbNodeReplacementHandlerDispatcher.dispatch(this, writeArtificialResNode.value());
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(WriteRegTensorNode writeRegTensorNode) {
    if (writeRegTensorNode.isDeleted()) {
      return;
    }

    for (var index : writeRegTensorNode.indices()) {
      LcbNodeReplacementHandlerDispatcher.dispatch(this, index);
    }

    LcbNodeReplacementHandlerDispatcher.dispatch(this, writeRegTensorNode.condition());
    LcbNodeReplacementHandlerDispatcher.dispatch(this, writeRegTensorNode.value());

    if (writeRegTensorNode.regTensor().isSingleRegister()
        && writeRegTensorNode.isPcAccess()) {
      if (writeRegTensorNode.value() instanceof BuiltInCall builtin && Set.of(
          BuiltInTable.ADD,
          BuiltInTable.ADDS,
          BuiltInTable.SUB
      ).contains(builtin.builtIn())) {
        // We need four parameters to replace a memory write by `LlvmBrCcSD`.
        // 1. the conditional code (SETEQ, ...)
        // 2. the first operand of the comparison
        // 3. the second operand of the comparison
        // 4. the immediate offset

        if (writeRegTensorNode.condition() instanceof BuiltInCall conditional) {
          for (var arg : conditional.arguments()) {
            LcbNodeReplacementHandlerDispatcher.dispatch(this, arg);
          }

          if (conditional.builtIn() == BuiltInTable.AND
              || conditional.builtIn() == BuiltInTable.OR) {
            DeferredDiagnosticStore.add(Diagnostic.warning(
                "Compiler generator is not able to lower a conjunction / disjunction. "
                    + "This will be skipped.",
                conditional.location()));
            return;
          }

          var condCond = LlvmCondCode.from(conditional.builtIn(), conditional.location());
          ensureNonNull(condCond,
              () -> Diagnostic.error("CondCode must not be null", conditional.location()));

          if (conditional.arguments().size() != 2) {
            return;
          }

          var first = conditional.arguments().get(0);
          var second = conditional.arguments().get(1);
          var immOffset =
              builtin.arguments().stream().filter(x -> x instanceof FieldAccessRefNode)
                  .findFirst();

          // Both arguments for the conditional must be registers.
          if (!(first instanceof ReadRegTensorNode && second instanceof ReadRegTensorNode)) {
            Objects.requireNonNull(writeRegTensorNode.graph()).add(new LlvmUnlowerableSD());
            return;
          }

          var hasOnlyOneImmOffset =
              builtin.arguments().stream().filter(x -> x instanceof FieldAccessRefNode).count()
                  == 1;
          var hasPC = builtin.arguments().stream()
              .filter(x -> x instanceof ReadRegTensorNode readRegTensorNode
                  && readRegTensorNode.isPcAccess()).count() == 1;
          var hasNoFields =
              builtin.arguments().stream().noneMatch(x -> x instanceof FieldRefNode);

          /*
           Check conditions s.t. this instruction matches
           ```
           instruction $name : Btype =                        // conditional branch instructions
              if (X(rs1) as $lhsTy) $relOp X(rs2) then
                PC := PC + immS
            ```

            but not

            ```
            instruction TEMP : Rtype =                        // 3 register operand instructions
              if NZCV_Z = 1 then
                PC := PC + X(rs1) + X(rs2) + shamt
            ```
           */
          if (!(hasOnlyOneImmOffset && hasNoFields && hasPC)) {
            Objects.requireNonNull(writeRegTensorNode.graph()).add(new LlvmUnlowerableSD());
            return;
          }

          if (immOffset.isEmpty()) {
            DeferredDiagnosticStore.add(
                Diagnostic.warning("Cannot find an immediate offset to make it a conditional jump",
                    builtin.location()).build());
            return;
          }

          writeRegTensorNode.value().replaceAndDelete(new LlvmBrCcSD(
              condCond,
              first,
              second,
              immOffset.get()
          ));
        } else {
          Objects.requireNonNull(writeRegTensorNode.graph()).add(new LlvmUnlowerableSD());
        }
      }
    }
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(ZeroExtendNode node) {
    if (node.value() instanceof ReadMemNode readMemNode) {
      node.replaceAndDelete(new LlvmZExtLoad(readMemNode));
      LcbNodeReplacementHandlerDispatcher.dispatch(this, readMemNode.address());
    } else {
      LcbNodeReplacementHandlerDispatcher.dispatch(this, node.value());
    }
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(LlvmUnlowerableSD node) {
    for (var arg : node.arguments()) {
      LcbNodeReplacementHandlerDispatcher.dispatch(this, arg);
    }
  }

  @Handler
  @SuppressWarnings("MissingJavadocMethod")
  public void handle(ExpressionNode node) {
    throw Diagnostic.error("not handled", node.location()).build();
  }
}
