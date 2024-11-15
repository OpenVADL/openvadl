package vadl.lcb.passes.llvmLowering.tablegen.model.parameterIdentity;

import static vadl.viam.ViamError.ensure;

import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBasicBlockSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmFieldAccessRefNode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmFrameIndexSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmReadRegFileNode;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.WriteRegFileNode;

/**
 * The idea of a parameter identity is that operands in the selection and machine pattern
 * can be both matched and replaced. This can be useful to change operands like {@code AddrFI}.
 */
public abstract class ParameterIdentity {
  /**
   * Render the parameter identity to a string.
   */
  public abstract String render();

  public static ParameterIdentity fromBasicBlockToImmediateLabel(LlvmBasicBlockSD basicBlockSD) {
    return new ParameterTypeAndNameIdentity(basicBlockSD.immediateOperand().rawName() + "AsLabel",
        basicBlockSD.fieldAccess().fieldRef().identifier.simpleName());
  }

  public static ParameterIdentity fromToImmediateLabel(LlvmFieldAccessRefNode node) {
    return new ParameterTypeAndNameIdentity(node.immediateOperand().rawName() + "AsLabel",
        node.fieldAccess().fieldRef().identifier.simpleName());
  }

  public static ParameterIdentity from(LlvmFieldAccessRefNode node) {
    return new ParameterTypeAndNameIdentity(node.immediateOperand().fullname(),
        node.fieldAccess().fieldRef().identifier.simpleName());
  }

  public static ParameterIdentity from(LlvmBasicBlockSD node) {
    return new ParameterTypeAndNameIdentity(node.lower(),
        node.fieldAccess().fieldRef().identifier.simpleName());
  }

  public static ParameterIdentity from(FieldRefNode node) {
    return new ParameterTypeAndNameIdentity(node.formatField().identifier.simpleName(),
        node.nodeName());
  }

  public static ParameterIdentity from(LlvmFrameIndexSD node, FieldRefNode address) {
    return new ParameterTypeAndNameIdentity(LlvmFrameIndexSD.NAME,
        address.formatField().identifier.simpleName());
  }

  public static ParameterIdentity from(WriteRegFileNode node, FieldRefNode address) {
    return new ParameterTypeAndNameIdentity(node.registerFile().simpleName(),
        address.formatField().identifier.simpleName());
  }

  public static ParameterIdentity from(ReadRegFileNode node, FieldRefNode address) {
    return new ParameterTypeAndNameIdentity(node.registerFile().simpleName(),
        address.formatField().identifier.simpleName());
  }

  public static ParameterIdentity from(LlvmReadRegFileNode node, FieldRefNode address) {
    return new ParameterTypeAndNameIdentity(node.registerFile().simpleName(),
        address.formatField().identifier.simpleName());
  }

  public static ParameterIdentity from(ReadRegFileNode node, FuncParamNode address) {
    return new ParameterTypeAndNameIdentity(node.registerFile().simpleName(),
        address.parameter().identifier.simpleName());
  }

  public static ParameterIdentity from(ReadRegFileNode node, ConstantNode address) {
    return new ParameterTypeAndNameIdentity(node.registerFile().simpleName(),
        address.constant().asVal().toString());
  }

  public static ParameterIdentity from(WriteRegFileNode node, FuncParamNode address) {
    return new ParameterTypeAndNameIdentity(node.registerFile().simpleName(),
        address.parameter().identifier.simpleName());
  }

  /**
   * Construct a parameter identity.
   */
  public static ParameterIdentity from(ReadRegFileNode node, ExpressionNode address) {
    ensure(address instanceof FieldRefNode
        || address instanceof ConstantNode
        || address instanceof FuncParamNode, "address must be a field or constant or func param");
    if (node instanceof LlvmFrameIndexSD frameIndexSD
        && address instanceof FieldRefNode fieldRefNode) {
      return ParameterIdentity.from(frameIndexSD, fieldRefNode);
    } else if (address instanceof FieldRefNode fieldRefNode) {
      return ParameterIdentity.from(node, fieldRefNode);
    } else if (address instanceof ConstantNode constantNode) {
      return ParameterIdentity.from(node, constantNode);
    } else {
      return ParameterIdentity.from(node, (FuncParamNode) address);
    }
  }
}
