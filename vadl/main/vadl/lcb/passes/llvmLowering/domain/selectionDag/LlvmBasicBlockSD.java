package vadl.lcb.passes.llvmLowering.domain.selectionDag;

import java.util.Objects;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.llvmLowering.LlvmNodeLowerable;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenMachineInstructionVisitor;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenNodeVisitor;
import vadl.lcb.passes.llvmLowering.tablegen.model.ParameterIdentity;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenImmediateRecord;
import vadl.lcb.template.lib.Target.Disassembler.EmitDisassemblerCppFilePass;
import vadl.lcb.template.lib.Target.MCTargetDesc.EmitMCCodeEmitterCppFilePass;
import vadl.types.Type;
import vadl.viam.Format;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.FieldAccessRefNode;

/**
 * LLVM node which represents the basic block as selection dag node.
 */
public class LlvmBasicBlockSD extends FieldAccessRefNode implements LlvmNodeLowerable {
  private final TableGenImmediateRecord immediateOperand;
  protected final ParameterIdentity parameterIdentity;

  /**
   * Creates an {@link LlvmBasicBlockSD} object that holds a reference to a format field
   * access. But in the selection dag, the immediate is a reference to a basic block.
   *
   * @param fieldAccess the format immediate to be referenced
   * @param type        of the node.
   */
  public LlvmBasicBlockSD(Format.FieldAccess fieldAccess, Type type) {
    super(fieldAccess, type);
    this.immediateOperand =
        new TableGenImmediateRecord(fieldAccess.fieldRef().identifier,
            Objects.requireNonNull(fieldAccess.encoding()).identifier.append(
                EmitMCCodeEmitterCppFilePass.WRAPPER),
            fieldAccess.accessFunction().identifier.append(EmitDisassemblerCppFilePass.WRAPPER),
            fieldAccess.predicate().identifier,
            ValueType.from(type));
    this.parameterIdentity = ParameterIdentity.from(this);
  }

  public TableGenImmediateRecord immediateOperand() {
    return immediateOperand;
  }

  @Override
  public Node copy() {
    return new LlvmBasicBlockSD(fieldAccess, type());
  }

  @Override
  public Node shallowCopy() {
    return new LlvmBasicBlockSD(fieldAccess, type());
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    if (visitor instanceof TableGenMachineInstructionVisitor v) {
      v.visit(this);
    }
    if (visitor instanceof TableGenNodeVisitor v) {
      v.visit(this);
    }
  }

  @Override
  public String lower() {
    return "bb";
  }
}
