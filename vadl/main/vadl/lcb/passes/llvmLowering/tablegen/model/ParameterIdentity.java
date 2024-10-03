package vadl.lcb.passes.llvmLowering.tablegen.model;

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
public record ParameterIdentity(String type, String name) {
  public String render() {
    return String.format("%s:$%s", type, name);
  }

  public static ParameterIdentity fromBasicBlockToImmediateLabel(LlvmBasicBlockSD basicBlockSD) {
    return new ParameterIdentity(basicBlockSD.immediateOperand().rawName() + "AsLabel",
        basicBlockSD.fieldAccess().fieldRef().identifier.simpleName());
  }

  public static ParameterIdentity from(LlvmFieldAccessRefNode node) {
    return new ParameterIdentity(node.immediateOperand().fullname(),
        node.fieldAccess().fieldRef().identifier.simpleName());
  }

  public static ParameterIdentity from(LlvmBasicBlockSD node) {
    return new ParameterIdentity(node.lower(),
        node.fieldAccess().fieldRef().identifier.simpleName());
  }

  public static ParameterIdentity from(FieldRefNode node) {
    return new ParameterIdentity(node.formatField().identifier.simpleName(),
        node.nodeName());
  }

  public static ParameterIdentity from(LlvmFrameIndexSD node, FieldRefNode address) {
    return new ParameterIdentity(LlvmFrameIndexSD.NAME,
        address.formatField().identifier.simpleName());
  }

  public static ParameterIdentity from(WriteRegFileNode node, FieldRefNode address) {
    return new ParameterIdentity(node.registerFile().name(),
        address.formatField().identifier.simpleName());
  }

  public static ParameterIdentity from(ReadRegFileNode node, FieldRefNode address) {
    return new ParameterIdentity(node.registerFile().name(),
        address.formatField().identifier.simpleName());
  }

  public static ParameterIdentity from(LlvmReadRegFileNode node, FieldRefNode address) {
    return new ParameterIdentity(node.registerFile().name(),
        address.formatField().identifier.simpleName());
  }

  public static ParameterIdentity from(ReadRegFileNode node, FuncParamNode address) {
    return new ParameterIdentity(node.registerFile().name(),
        address.parameter().identifier.simpleName());
  }

  public static ParameterIdentity from(ReadRegFileNode node, ConstantNode address) {
    return new ParameterIdentity(node.registerFile().name(),
        address.constant().asVal().toString());
  }

  public static ParameterIdentity from(WriteRegFileNode node, FuncParamNode address) {
    return new ParameterIdentity(node.registerFile().name(),
        address.parameter().identifier.simpleName());
  }

  public static ParameterIdentity from(ReadRegFileNode node, ExpressionNode address) {
    ensure(address instanceof FieldRefNode
        || address instanceof ConstantNode
        || address instanceof FuncParamNode, "address must be a field or constant or func param");
    if (node instanceof LlvmFrameIndexSD frameIndexSD &&
        address instanceof FieldRefNode fieldRefNode) {
      return ParameterIdentity.from(frameIndexSD, fieldRefNode);
    } else if (address instanceof FieldRefNode fieldRefNode) {
      return ParameterIdentity.from(node, fieldRefNode);
    } else if (address instanceof ConstantNode constantNode) {
      return ParameterIdentity.from(node, constantNode);
    } else {
      return ParameterIdentity.from(node, (FuncParamNode) address);
    }
  }

  public ParameterIdentity withType(String type) {
    return new ParameterIdentity(type, name);
  }
}
