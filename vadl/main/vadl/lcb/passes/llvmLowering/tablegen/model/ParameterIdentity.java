package vadl.lcb.passes.llvmLowering.tablegen.model;

import vadl.lcb.passes.llvmLowering.model.LlvmFieldAccessRefNode;
import vadl.lcb.passes.llvmLowering.model.LlvmFrameIndexSD;
import vadl.lcb.passes.llvmLowering.model.LlvmReadRegFileNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.WriteRegFileNode;

/**
 * {@code X:$rs2} and {@code AddrFI:$rs1} are both
 * parameter identifies in the pattern.
 * def : Pat<(truncstorei8 X:$rs2, AddrFI:$rs1),
 */
public record ParameterIdentity(String type, String name) {
  public String render() {
    return String.format("%s:$%s", type, name);
  }

  public static ParameterIdentity from(LlvmFieldAccessRefNode node) {
    return new ParameterIdentity(node.immediateOperand().fullname(),
        node.fieldAccess().identifier.simpleName());
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


  public ParameterIdentity withType(String type) {
    return new ParameterIdentity(type, name);
  }
}
