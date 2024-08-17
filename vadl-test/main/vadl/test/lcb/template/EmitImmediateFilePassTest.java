package vadl.test.lcb.template;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vadl.gcb.passes.encoding_generation.GenerateFieldAccessEncodingFunctionPass;
import vadl.gcb.passes.field_node_replacement.FieldNodeReplacementPassForDecoding;
import vadl.gcb.passes.type_normalization.CppTypeNormalizationForDecodingsPass;
import vadl.gcb.passes.type_normalization.CppTypeNormalizationForEncodingsPass;
import vadl.gcb.passes.type_normalization.CppTypeNormalizationForPredicatesPass;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.lcb.template.lib.Target.Utils.EmitImmediateFilePass;
import vadl.pass.PassKey;
import vadl.pass.PassManager;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.test.AbstractTest;
import vadl.viam.passes.typeCastElimination.TypeCastEliminationPass;

public class EmitImmediateFilePassTest extends AbstractTest {
  @Test
  void testLowering() throws IOException, DuplicatedPassKeyException {
    // Given
    var passManager = new PassManager();
    var spec = runAndGetViamSpecification("examples/rv3264im.vadl");

    passManager.add(new PassKey("ty"), new TypeCastEliminationPass());
    passManager.add(new PassKey("encoding"), new GenerateFieldAccessEncodingFunctionPass());
    passManager.add(new PassKey("fieldDecoding"), new FieldNodeReplacementPassForDecoding());
    passManager.add(new PassKey(CppTypeNormalizationForPredicatesPass.class.toString()),
        new CppTypeNormalizationForPredicatesPass());
    passManager.add(new PassKey(CppTypeNormalizationForDecodingsPass.class.toString()),
        new CppTypeNormalizationForDecodingsPass());
    passManager.add(new PassKey(CppTypeNormalizationForEncodingsPass.class.toString()),
        new CppTypeNormalizationForEncodingsPass());

    passManager.run(spec);

    // When
    var template =
        new EmitImmediateFilePass(createLcbConfiguration(),
            new ProcessorName("processorNameValue"));
    var writer = new StringWriter();

    // When
    template.renderToString(passManager.getPassResults(), spec, writer);
    var output = writer.toString();

    Assertions.assertEquals("""
        """, output);
  }
}
