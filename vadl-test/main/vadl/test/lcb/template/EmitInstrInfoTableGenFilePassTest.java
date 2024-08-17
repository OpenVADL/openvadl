package vadl.test.lcb.template;

import java.io.IOException;
import java.io.StringWriter;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vadl.gcb.passes.encoding_generation.GenerateFieldAccessEncodingFunctionPass;
import vadl.gcb.passes.field_node_replacement.FieldNodeReplacementPassForDecoding;
import vadl.gcb.passes.type_normalization.CppTypeNormalizationForDecodingsPass;
import vadl.gcb.passes.type_normalization.CppTypeNormalizationForEncodingsPass;
import vadl.gcb.passes.type_normalization.CppTypeNormalizationForPredicatesPass;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.lcb.passes.isaMatching.IsaMatchingPass;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.template.lib.Target.Utils.EmitImmediateFilePass;
import vadl.pass.PassKey;
import vadl.pass.PassManager;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.test.AbstractTest;
import vadl.viam.passes.FunctionInlinerPass;
import vadl.viam.passes.typeCastElimination.TypeCastEliminationPass;

public class EmitInstrInfoTableGenFilePassTest extends AbstractTest {
  @Test
  void testLowering() throws IOException, DuplicatedPassKeyException {
    // Given
    var passManager = new PassManager();
    var spec = runAndGetViamSpecification("examples/rv3264im.vadl");

    passManager.add(new TypeCastEliminationPass());
    passManager.add(new FunctionInlinerPass());
    passManager.add(new IsaMatchingPass());
    passManager.add(new LlvmLoweringPass());

    passManager.run(spec);

    // When
    var template =
        new vadl.lcb.lib.Target.EmitInstrInfoTableGenFilePass(createLcbConfiguration(),
            new ProcessorName("processorNameValue"));
    var writer = new StringWriter();

    // When
    template.renderToString(passManager.getPassResults(), spec, writer);
    var output = writer.toString();

    Assertions.assertEquals("""
        """, output);
  }
}
