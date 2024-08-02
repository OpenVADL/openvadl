package vadl.lcb.tablegen.decorators;

import java.io.StringWriter;
import vadl.lcb.codegen.mappers.ImmediateOperandExtractor;
import vadl.lcb.tablegen.lowering.TableGenImmediateOperandRenderer;
import vadl.lcb.tablegen.model.TableGenImmediateOperand;
import vadl.viam.Specification;

/**
 * Writes all the immediate operands into the TableGenFile.
 */
public class TableGenWithImmediateOperandsDecorator extends TableGenAbstractDecorator {

  public TableGenWithImmediateOperandsDecorator(TableGenAbstractDecorator parent) {
    super(parent);
  }

  @Override
  public void render(StringWriter writer, Specification specification) {
    if (parent != null) {
      parent.render(writer, specification);
    }

    ImmediateOperandExtractor.extract(specification)
        .map(immediateOperand -> new TableGenImmediateOperand(immediateOperand.name(),
            immediateOperand.encoderMethod(),
            immediateOperand.decoderMethod(), immediateOperand.type()))
        .forEach(imm -> {
          var lowered = TableGenImmediateOperandRenderer.lower(imm);
          writer.write(lowered + "\n");
        });
  }
}
